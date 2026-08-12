package com.ssafy.b109.aivo.interview.service;

import com.ssafy.b109.aivo.global.exception.CustomException;
import com.ssafy.b109.aivo.global.exception.ErrorCode;
import com.ssafy.b109.aivo.global.rabbitmq.config.RabbitMQConfig;
import com.ssafy.b109.aivo.interview.dto.InterviewReportJobResponse;
import com.ssafy.b109.aivo.interview.entity.Interview;
import com.ssafy.b109.aivo.interview.entity.InterviewReportJob;
import com.ssafy.b109.aivo.interview.entity.InterviewReportJobStatus;
import com.ssafy.b109.aivo.interview.repository.InterviewReportJobRepository;
import com.ssafy.b109.aivo.media.entity.Audio;
import com.ssafy.b109.aivo.media.repository.AudioRepository;
import com.ssafy.b109.aivo.practice.entity.Practice;
import com.ssafy.b109.aivo.practice.repository.PracticeRepository;
import com.ssafy.b109.aivo.rabbitmq.dto.AudioAnalysisCompletedMessage;
import com.ssafy.b109.aivo.rabbitmq.dto.AudioSTTRequest;
import com.ssafy.b109.aivo.rabbitmq.entity.AnalysisEventType;
import com.ssafy.b109.aivo.rabbitmq.entity.EventType;
import com.ssafy.b109.aivo.rabbitmq.publisher.AnalysisMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewAsyncReportService {

    private final InterviewReportJobRepository interviewReportJobRepository;
    private final PracticeRepository practiceRepository;
    private final AudioRepository audioRepository;
    private final AnalysisMessagePublisher analysisMessagePublisher;
    private final InterviewReportService interviewReportService;

    @Transactional
    public InterviewReportJob createOrGetPendingJob(
            Interview interview,
            Practice practice,
            Audio audio
    ) {
        return interviewReportJobRepository
                .findByPracticeId(practice.getId())
                .orElseGet(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    InterviewReportJob job = new InterviewReportJob();
                    job.setInterviewId(interview.getId());
                    job.setPracticeId(practice.getId());
                    job.setAudioId(audio == null ? null : audio.getId());
                    job.setRequestId(UUID.randomUUID());
                    job.setStatus(InterviewReportJobStatus.PENDING);
                    job.setCreatedAt(now);
                    job.setUpdatedAt(now);
                    return interviewReportJobRepository.saveAndFlush(job);
                });
    }

    @Transactional(readOnly = true)
    public InterviewReportJobResponse getLatestStatus(Long interviewId) {
        InterviewReportJob job = interviewReportJobRepository
                .findFirstByInterviewIdOrderByCreatedAtDescIdDesc(interviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_INTERVIEW_REPORT));

        return InterviewReportJobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public Optional<InterviewReportJob> findByPracticeId(Long practiceId) {
        return interviewReportJobRepository.findByPracticeId(practiceId);
    }

    public InterviewReportJobResponse toResponse(InterviewReportJob job) {
        return InterviewReportJobResponse.from(job);
    }

    public void publishAudioAnalysisRequest(Long jobId) {
        try {
            InterviewReportJob job = markStatus(jobId, InterviewReportJobStatus.STT_ANALYZING, null);
            if (job.getAudioId() == null) {
                generateReportAsync(job.getPracticeId());
                return;
            }

            Audio audio = audioRepository.findById(job.getAudioId())
                    .orElseThrow(() -> new CustomException(ErrorCode.INVALID_AUDIO_ANALYSIS_REQUEST));

            AudioSTTRequest request = new AudioSTTRequest(
                    AnalysisEventType.AUDIO_ANALYSIS_REQUEST,
                    job.getRequestId(),
                    job.getPracticeId(),
                    audio.getId(),
                    Instant.now(),
                    audio.getPath()
            );

            analysisMessagePublisher.publish(
                    RabbitMQConfig.AUDIO_ROUTING_KEY,
                    EventType.AUDIO_ANALYSIS_REQUEST,
                    job.getRequestId(),
                    request
            );
        } catch (Exception exception) {
            markFailed(jobId, exception);
        }
    }

    @Async
    public void generateReportAsync(Long practiceId) {
        generateReportIfInterviewPractice(practiceId);
    }

    public boolean generateReportIfInterviewPractice(Long practiceId) {
        Practice practice = practiceRepository.findById(practiceId)
                .orElse(null);
        if (practice == null || practice.getInterviewSession() == null) {
            return false;
        }

        InterviewReportJob job = interviewReportJobRepository
                .findByPracticeId(practiceId)
                .orElse(null);
        if (job == null || job.getStatus() == InterviewReportJobStatus.COMPLETED) {
            return true;
        }

        try {
            markStatus(job.getId(), InterviewReportJobStatus.LLM_ANALYZING, null);
            interviewReportService.createReportFromSavedData(
                    practice.getInterviewSession(),
                    practice
            );
            markStatus(job.getId(), InterviewReportJobStatus.COMPLETED, null);
        } catch (Exception exception) {
            log.warn(
                    "Interview async report generation failed: practiceId={}, jobId={}",
                    practiceId,
                    job.getId(),
                    exception
            );
            markFailed(job.getId(), exception);
        }
        return true;
    }

    public boolean markFailedIfInterviewJob(AudioAnalysisCompletedMessage message) {
        UUID requestId = parseRequestId(message.requestId());
        if (requestId == null) {
            return false;
        }

        return interviewReportJobRepository.findByRequestId(requestId)
                .map(job -> {
                    markFailed(job.getId(), message.errorMessage());
                    return true;
                })
                .orElse(false);
    }

    public boolean markFailedIfInterviewJob(AudioAnalysisCompletedMessage message, Exception exception) {
        UUID requestId = parseRequestId(message.requestId());
        if (requestId == null) {
            return false;
        }

        return interviewReportJobRepository.findByRequestId(requestId)
                .map(job -> {
                    markFailed(job.getId(), exception);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public InterviewReportJob markStatus(
            Long jobId,
            InterviewReportJobStatus status,
            String errorMessage
    ) {
        InterviewReportJob job = interviewReportJobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_INTERVIEW_REPORT));
        job.setStatus(status);
        job.setErrorMessage(errorMessage);
        job.setUpdatedAt(LocalDateTime.now());
        return interviewReportJobRepository.saveAndFlush(job);
    }

    public void markFailed(Long jobId, Exception exception) {
        markFailed(jobId, exception.getMessage());
    }

    public void markFailed(Long jobId, String errorMessage) {
        try {
            markStatus(
                    jobId,
                    InterviewReportJobStatus.FAILED,
                    trimError(errorMessage)
            );
        } catch (Exception ignored) {
            log.warn("Interview report job failure status update failed: jobId={}", jobId, ignored);
        }
    }

    private UUID parseRequestId(String requestId) {
        try {
            return requestId == null ? null : UUID.fromString(requestId);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String trimError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return null;
        }
        return errorMessage.length() > 1000
                ? errorMessage.substring(0, 1000)
                : errorMessage;
    }
}
