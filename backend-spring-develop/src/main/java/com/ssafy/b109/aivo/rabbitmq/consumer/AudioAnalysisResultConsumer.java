package com.ssafy.b109.aivo.rabbitmq.consumer;

import com.ssafy.b109.aivo.global.rabbitmq.config.RabbitMQConfig;
import com.ssafy.b109.aivo.interview.service.InterviewAsyncReportService;
import com.ssafy.b109.aivo.presentation.entity.PresentationReportJob;
import com.ssafy.b109.aivo.presentation.entity.PresentationReportJobStatus;
import com.ssafy.b109.aivo.presentation.repository.PresentationReportJobRepository;
import com.ssafy.b109.aivo.presentation.service.PresentationAnalysisService;
import com.ssafy.b109.aivo.presentation.service.PresentationReportJobStatusService;
import com.ssafy.b109.aivo.presentation.service.PresentationScoreService;
import com.ssafy.b109.aivo.rabbitmq.dto.AudioAnalysisCompletedMessage;
import com.ssafy.b109.aivo.rabbitmq.service.AudioAnalysisResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AudioAnalysisResultConsumer {

    private final AudioAnalysisResultService audioAnalysisResultService;
    private final PresentationAnalysisService presentationAnalysisService;
    private final PresentationScoreService presentationScoreService;
    private final InterviewAsyncReportService interviewAsyncReportService;
    private final PresentationReportJobRepository presentationReportJobRepository;
    private final PresentationReportJobStatusService presentationReportJobStatusService;

    @RabbitListener(queues = RabbitMQConfig.RESULT_QUEUE)
    public void consume(AudioAnalysisCompletedMessage message) {
        log.info(
                "Spring 오디오 분석 결과 수신: eventType={}, requestId={}, practiceId={}, audioId={}, segmentCount={}",
                message.eventType(),
                message.requestId(),
                message.practiceId(),
                message.audioId(),
                message.segments() == null ? 0 : message.segments().size()
        );

        if (message.failed()) {
            if (interviewAsyncReportService.markFailedIfInterviewJob(message)) {
                return;
            }
            presentationReportJobStatusService.markFailed(message.practiceId(), message.errorMessage());
            throw new IllegalStateException(message.errorMessage());
        }

        try {
            audioAnalysisResultService.saveAudioStt(message);
        } catch (Exception exception) {
            if (interviewAsyncReportService.markFailedIfInterviewJob(message, exception)) {
                return;
            }
            presentationReportJobStatusService.markFailed(message.practiceId(), exception);
            throw exception;
        }

        if (interviewAsyncReportService.generateReportIfInterviewPractice(
                message.practiceId()
        )) {
            return;
        }

        try {
            presentationAnalysisService.generateSlideFeedbacksIfPresentationPractice(
                    message.practiceId()
            );
            presentationScoreService.setPresentationScore(
                    message.practiceId()
            );
        } catch (Exception exception) {
            presentationReportJobStatusService.markFailed(message.practiceId(), exception);
            throw exception;
        }
        PresentationReportJob presentationReportJob =
                presentationReportJobRepository.findByPracticeId(message.practiceId())
                        .orElseThrow(()-> new RuntimeException("presentationReportJob을 찾을 수 없습니다."));
        presentationReportJob.setStatus(PresentationReportJobStatus.COMPLETED);
        presentationReportJob.setUpdatedAt(LocalDateTime.now());
        presentationReportJobRepository.saveAndFlush(presentationReportJob);
    }
}
