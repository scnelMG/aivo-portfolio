import asyncio
import json
import logging
from collections.abc import Awaitable, Callable
from typing import Any

from aio_pika import ExchangeType, IncomingMessage
from aio_pika.abc import AbstractRobustChannel, AbstractRobustQueue
from pydantic import ValidationError

from app.core.config import settings
from app.messaging.contracts import (
    AiEvent,
    ErrorCode,
    EventStatus,
    TaskType,
    WorkerType,
    create_completed_event,
    create_failed_event,
    parse_event_bytes,
    parse_spring_audio_stt_request_bytes,
    validation_error_message,
)
from app.messaging.constants import AUDIO_ROUTING_KEY, RESULT_ROUTING_KEY
from app.messaging.rabbitmq import RabbitMQClient, queue_arguments_for
from app.messaging.result_publisher import ResultPublisher

logger = logging.getLogger(__name__)

HandlerMap = dict[
    TaskType,
    Callable[[dict[str, Any]], Awaitable[dict[str, Any]]],
]


class RabbitWorker:
    def __init__(
        self,
        worker_name: str,
        worker_type: WorkerType,
        queue_name: str,
        binding_key: str,
        prefetch_count: int,
        handlers: HandlerMap,
        rabbitmq_client: RabbitMQClient | None = None,
    ) -> None:
        self.worker_name = worker_name
        self.worker_type = worker_type
        self.queue_name = queue_name
        self.binding_key = binding_key
        self.prefetch_count = prefetch_count
        self.handlers = handlers
        self.rabbitmq_client = rabbitmq_client or RabbitMQClient()
        self.result_publisher = ResultPublisher(self.rabbitmq_client)
        self._consumer_channel: AbstractRobustChannel | None = None
        self._queue: AbstractRobustQueue | None = None

    async def start(self) -> None:
        await self.rabbitmq_client.connect(self.worker_name)
        await self.rabbitmq_client.declare_topology()
        self._consumer_channel = await self.rabbitmq_client.create_consumer_channel(
            prefetch_count=self.prefetch_count,
        )
        exchange = await self._consumer_channel.declare_exchange(
            settings.rabbitmq_exchange,
            ExchangeType.DIRECT,
            durable=True,
            auto_delete=False,
        )
        self._queue = await self._consumer_channel.declare_queue(
            self.queue_name,
            durable=True,
            auto_delete=False,
            exclusive=False,
            arguments=queue_arguments_for(self.queue_name),
        )
        await self._queue.bind(exchange, routing_key=self.binding_key)
        await self._queue.consume(self._on_message, no_ack=False)
        logger.info(
            "%s started: queue=%s bindingKey=%s prefetch=%s",
            self.worker_name,
            self.queue_name,
            self.binding_key,
            self.prefetch_count,
        )

    async def run_forever(self) -> None:
        while True:
            try:
                await self.start()
                break
            except asyncio.CancelledError:
                raise
            except Exception:
                logger.exception(
                    "%s failed to start. Retrying in 5 seconds",
                    self.worker_name,
                )
                await self.close()
                await asyncio.sleep(5)

        try:
            await asyncio.Event().wait()
        except asyncio.CancelledError:
            logger.info("%s cancellation requested", self.worker_name)
            raise
        finally:
            await self.close()

    async def close(self) -> None:
        if self._consumer_channel and not self._consumer_channel.is_closed:
            await self._consumer_channel.close()
        await self.rabbitmq_client.close()
        logger.info("%s stopped", self.worker_name)

    async def handle_event(self, event: AiEvent) -> AiEvent:
        if event.status != EventStatus.REQUESTED:
            return create_failed_event(
                event,
                ErrorCode.INVALID_EVENT_STATUS,
                f"Worker only accepts REQUESTED events. received={event.status.value}",
                retryable=False,
            )

        if event.workerType != self.worker_type:
            return create_failed_event(
                event,
                ErrorCode.INVALID_WORKER_TYPE,
                f"Event workerType={event.workerType.value} cannot be handled by {self.worker_type.value}",
                retryable=False,
            )

        handler = self.handlers.get(event.taskType)
        if handler is None:
            return create_failed_event(
                event,
                ErrorCode.UNSUPPORTED_TASK,
                f"Unsupported taskType for {self.worker_type.value}: {event.taskType.value}",
                retryable=False,
            )

        try:
            payload = await handler(event.payload)
            return create_completed_event(event, payload)
        except ValidationError as error:
            return create_failed_event(
                event,
                ErrorCode.INVALID_PAYLOAD,
                validation_error_message(error),
                retryable=False,
            )
        except Exception as error:
            logger.exception(
                "AI handler failed: worker=%s eventId=%s taskType=%s",
                self.worker_name,
                event.eventId,
                event.taskType.value,
            )
            return create_failed_event(
                event,
                ErrorCode.AI_PROCESSING_FAILED,
                str(error),
                retryable=False,
            )

    async def _on_message(self, message: IncomingMessage) -> None:
        logger.info(
            "------- FastAPI RabbitMQ 메시지 수신 시작 worker=%s routingKey=%s ----------",
            self.worker_name,
            message.routing_key,
        )
        if self._is_spring_audio_message(message):
            await self._on_spring_audio_message(message)
            return

        try:
            event = parse_event_bytes(message.body)
        except (UnicodeDecodeError, json.JSONDecodeError, ValidationError) as error:
            logger.warning(
                "Rejecting invalid RabbitMQ event: worker=%s routingKey=%s error=%s",
                self.worker_name,
                message.routing_key,
                error,
            )
            await message.reject(requeue=False)
            return

        logger.info(
            "Received RabbitMQ event: worker=%s eventId=%s jobId=%s taskType=%s",
            self.worker_name,
            event.eventId,
            event.jobId,
            event.taskType.value,
        )
        logger.info(
            "------- FastAPI RabbitMQ 메시지 수신 완료 worker=%s eventId=%s ----------",
            self.worker_name,
            event.eventId,
        )

        result_event = await self.handle_event(event)

        try:
            await self.result_publisher.publish(result_event)
        except Exception:
            logger.exception(
                "Result publish failed. NACK request: worker=%s eventId=%s",
                self.worker_name,
                event.eventId,
            )
            await message.nack(requeue=True)
            return

        await message.ack()
        logger.info(
            "ACK request after result publish: worker=%s requestEventId=%s resultEventId=%s status=%s",
            self.worker_name,
            event.eventId,
            result_event.eventId,
            result_event.status.value,
        )

    def _is_spring_audio_message(self, message: IncomingMessage) -> bool:
        if self.worker_type != WorkerType.AUDIO or message.routing_key != AUDIO_ROUTING_KEY:
            return False

        try:
            raw = json.loads(message.body.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError):
            return False

        return raw.get("eventType") == "AUDIO_ANALYSIS_REQUEST" and "requestId" in raw

    async def _on_spring_audio_message(self, message: IncomingMessage) -> None:
        try:
            request = parse_spring_audio_stt_request_bytes(message.body)
        except (UnicodeDecodeError, json.JSONDecodeError, ValidationError) as error:
            logger.warning(
                "Rejecting invalid Spring audio request: worker=%s routingKey=%s error=%s",
                self.worker_name,
                message.routing_key,
                error,
            )
            await message.reject(requeue=False)
            return

        logger.info(
            "Received Spring audio STT request: worker=%s requestId=%s practiceId=%s audioId=%s",
            self.worker_name,
            request.requestId,
            request.practiceId,
            request.audioId,
        )
        logger.info(
            "------- FastAPI Spring 오디오 메시지 수신 완료 worker=%s requestId=%s ----------",
            self.worker_name,
            request.requestId,
        )

        try:
            service = getattr(self, "spring_audio_service")
            result = await service.handle_spring_audio_stt(request)
            await self.rabbitmq_client.publish_json(
                result.model_dump(mode="json"),
                RESULT_ROUTING_KEY,
                message_id=str(request.requestId),
                correlation_id=str(request.requestId),
            )
        except Exception as error:
            logger.exception(
                "Spring audio STT processing failed. NACK request: worker=%s requestId=%s",
                self.worker_name,
                request.requestId,
            )
            try:
                await self.rabbitmq_client.publish_json(
                    {
                        "eventType": "AUDIO_ANALYSIS_FAILED",
                        "requestId": str(request.requestId),
                        "practiceId": request.practiceId,
                        "audioId": request.audioId,
                        "segments": [],
                        "errorCode": "AUDIO_ANALYSIS_FAILED",
                        "errorMessage": str(error),
                    },
                    RESULT_ROUTING_KEY,
                    message_id=str(request.requestId),
                    correlation_id=str(request.requestId),
                )
            except Exception:
                logger.exception(
                    "Spring audio STT failure publish failed. NACK request: worker=%s requestId=%s",
                    self.worker_name,
                    request.requestId,
                )
                await message.nack(requeue=True)
                return

            await message.ack()
            return

        await message.ack()
        logger.info(
            "ACK Spring audio request after result publish: worker=%s requestId=%s segmentCount=%s",
            self.worker_name,
            request.requestId,
            len(result.segments),
        )
