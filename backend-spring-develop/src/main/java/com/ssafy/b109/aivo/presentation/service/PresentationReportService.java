package com.ssafy.b109.aivo.presentation.service;

import com.ssafy.b109.aivo.feedback.entity.TotalFeedback;
import com.ssafy.b109.aivo.feedback.repository.TotalFeedbackRepository;
import com.ssafy.b109.aivo.global.exception.CustomException;
import com.ssafy.b109.aivo.global.exception.ErrorCode;
import com.ssafy.b109.aivo.media.entity.Audio;
import com.ssafy.b109.aivo.media.entity.AudioSegment;
import com.ssafy.b109.aivo.media.entity.AudioStt;
import com.ssafy.b109.aivo.media.entity.Video;
import com.ssafy.b109.aivo.media.repository.AudioRepository;
import com.ssafy.b109.aivo.media.repository.AudioSegmentRepository;
import com.ssafy.b109.aivo.media.repository.AudioSttRepository;
import com.ssafy.b109.aivo.media.repository.VideoRepository;
import com.ssafy.b109.aivo.nonverbal.entity.NonverbalAnalysisLog;
import com.ssafy.b109.aivo.nonverbal.repository.NonverbalAnalysisLogRepository;
import com.ssafy.b109.aivo.portfolio.util.S3PortfolioUploader;
import com.ssafy.b109.aivo.practice.entity.Practice;
import com.ssafy.b109.aivo.practice.repository.PracticeRepository;
import com.ssafy.b109.aivo.presentation.dto.*;
import com.ssafy.b109.aivo.presentation.entity.*;
import com.ssafy.b109.aivo.presentation.repository.*;
import com.ssafy.b109.aivo.presentation.util.S3SlideImageStorage;
import com.ssafy.b109.aivo.slide.entity.SlideClickLog;
import com.ssafy.b109.aivo.slide.repository.SlideClickLogRepository;
import com.ssafy.b109.aivo.speech.entity.SpeechAnalysisLog;
import com.ssafy.b109.aivo.speech.repository.SpeechAnalysisLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PresentationReportService {

    private final PracticeRepository practiceRepository;
    private final PresentationSlideRepository presentationSlideRepository;
    private final PresentationScoreRepository presentationScoreRepository;
    private final VideoRepository videoRepository;
    private final AudioRepository audioRepository;
    private final AudioSttRepository audioSttRepository;
    private final AudioSegmentRepository audioSegmentRepository;
    private final SlideClickLogRepository slideClickLogRepository;
    private final SpeechAnalysisLogRepository speechAnalysisLogRepository;
    private final PresentationSlideFeedbackRepository presentationSlideFeedbackRepository;
    private final TotalFeedbackRepository totalFeedbackRepository;
    private final PresentationQuestionRepository presentationQuestionRepository;
    private final PresentationQuestionFeedbackRepository presentationQuestionFeedbackRepository;
    private final NonverbalAnalysisLogRepository nonverbalAnalysisLogRepository;
    private final S3SlideImageStorage s3SlideImageStorage;
    private final S3PortfolioUploader s3PortfolioUploader;
    private final ObjectMapper objectMapper;

    public PresentationReportResponse getReport(Long userId, Long presentationId) {
        Practice practice =
                findOwnedPracticeByPresentation(
                        userId,
                        presentationId
                );

        Presentation presentation =
                practice.getPresentation();

        if (presentation == null) {
            throw new CustomException(
                    ErrorCode.PRESENTATION_NOT_FOUND
            );
        }

        return new PresentationReportResponse(
                buildPracticeResponse(practice),
                buildPresentationResponse(presentation),
                buildScoreResponse(practice),
                buildMediaResponse(practice),
                buildAudioSttResponse(practice),
                buildSpeechAnalysisResponse(practice),
                buildNonverbalAnalysisResponse(practice),
                buildSlideResponses(practice, presentation),
                buildQuestionAnswerResponses(practice, presentation)
        );
    }

    private Practice findOwnedPracticeByPresentation(
            Long userId,
            Long presentationId
    ) {
        return practiceRepository
                .findByPresentation_IdAndFolder_User_Id(
                        presentationId,
                        userId
                )
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.PRESENTATION_NOT_FOUND
                        )
                );
    }

    private PresentationReportPracticeResponse buildPracticeResponse(
            Practice practice
    ) {

        return new PresentationReportPracticeResponse(
                practice.getId(),
                practice.getTitle(),
                practice.getDescription(),
                practice.getCreatedAt(),
                practice.getDurationSec()
        );
    }

    private PresentationReportPresentationResponse buildPresentationResponse(
            Presentation presentation
    ) {
        long slideCount = presentationSlideRepository
                .countByPresentationId(
                        presentation.getId()
                );

        return new PresentationReportPresentationResponse(
                presentation.getId(),
                presentation.getTargetDurationSec(),
                presentation.isAiQnaEnabled(),
                slideCount
        );
    }

    private PresentationReportScoreResponse buildScoreResponse(
            Practice practice
    ) {
        PresentationScore score =
                presentationScoreRepository
                        .findByPracticeId(
                                practice.getId()
                        )
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.PRESENTATION_REPORT_NOT_FOUND
                                )
                        );

        Double folderAverageScore =
                presentationScoreRepository
                        .findFolderAverageScore(
                                practice.getFolder().getId()
                        );

        double roundedAverage =
                roundToOneDecimal(
                        folderAverageScore == null
                                ? 0.0
                                : folderAverageScore
                );

        double averageDelta =
                roundToOneDecimal(
                        score.getOverallScore()
                        - roundedAverage
                );

        return new PresentationReportScoreResponse(
                score.getOverallScore(),
                roundedAverage,
                averageDelta,
                score.getContentScore(),
                score.getVoiceScore(),
                score.getVideoScore(),
                score.getQuestionAnswer()
        );
    }

    private double roundToOneDecimal(
            double value
    ) {
        return Math.round(value * 10.0) / 10.0;
    }

    private PresentationReportMediaResponse buildMediaResponse(
            Practice practice
    ) {
        PresentationReportVideoResponse videoResponse =
                videoRepository
                        .findByPracticeId(practice.getId())
                        .map(this::buildVideoResponse)
                        .orElse(null);

        PresentationReportAudioResponse audioResponse =
                audioRepository
                        .findByPracticeId(practice.getId())
                        .map(this::buildAudioResponse)
                        .orElse(null);

        return new PresentationReportMediaResponse(
                videoResponse,
                audioResponse
        );
    }

    private PresentationReportAudioResponse buildAudioResponse(
            Audio audio
    ) {
        return new PresentationReportAudioResponse(
                audio.getId(),
                audio.getType(),
                audio.getSize()
        );
    }

    private PresentationReportVideoResponse buildVideoResponse(
            Video video
    ) {
        String playbackUrl =
                s3PortfolioUploader.createReadUrl(
                        video.getPath(),
                        video.getType()
                );

        return new PresentationReportVideoResponse(
                video.getId(),
                video.getType(),
                video.getSize(),
                playbackUrl
        );
    }

    private PresentationReportAudioSttResponse buildAudioSttResponse(
            Practice practice
    ) {
        Audio audio =
                audioRepository
                        .findByPracticeId(practice.getId())
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.PRESENTATION_REPORT_NOT_FOUND
                                )
                        );

        AudioStt audioStt =
                audioSttRepository
                        .findFirstByAudioIdOrderByIdDesc(
                                audio.getId()
                        )
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.PRESENTATION_REPORT_NOT_FOUND
                                )
                        );

        List<AudioSegment> segments =
                audioSegmentRepository
                        .findAllByAudio_IdOrderBySequenceAscIdAsc(
                                audio.getId()
                        );

        List<SlideClickLog> slideLogs =
                slideClickLogRepository
                        .findAllByPracticeIdOrderByOccurredTimeMsAsc(
                                practice.getId()
                        );

        List<PresentationReportAudioSegmentResponse> segmentResponses =
                mapSegmentsToSlides(
                        segments,
                        slideLogs
                );

        return new PresentationReportAudioSttResponse(
                audioStt.getId(),
                audio.getId(),
                audioStt.getContent(),
                audioStt.getCreatedAt(),
                segmentResponses
        );
    }

    private List<PresentationReportAudioSegmentResponse> mapSegmentsToSlides(
            List<AudioSegment> segments,
            List<SlideClickLog> slideLogs
    ) {
        List<PresentationReportAudioSegmentResponse> responses =
                new ArrayList<>();

        int logIndex = 0;
        Long currentSlideId = null;

        for (AudioSegment segment : segments) {

            while (
                    logIndex < slideLogs.size()
                    && slideLogs
                               .get(logIndex)
                               .getOccurredTimeMs()
                       <= segment.getStartTimeMs()
            ) {
                SlideClickLog log =
                        slideLogs.get(logIndex);

                currentSlideId =
                        log.getToSlideId();

                logIndex++;
            }

            responses.add(
                    new PresentationReportAudioSegmentResponse(
                            segment.getText(),
                            segment.getStartSec(),
                            segment.getEndSec(),
                            segment.getStartTimeMs(),
                            segment.getEndTimeMs(),
                            currentSlideId
                    )
            );
        }

        return responses;
    }

    private PresentationReportSpeechAnalysisResponse buildSpeechAnalysisResponse(
            Practice practice
    ) {
        List<SpeechAnalysisLog> logs =
                speechAnalysisLogRepository
                        .findAllByPracticeIdOrderByStartTimeMsAscIdAsc(
                                practice.getId()
                        );

        List<PresentationReportSpeechWindowResponse> windows =
                buildSpeechWindows(logs);

        List<PresentationReportFillerBreakdownResponse> fillerBreakdown =
                buildFillerBreakdown(windows);

        int averageWpm =
                (int) Math.round(
                        windows.stream()
                                .map(
                                        PresentationReportSpeechWindowResponse
                                                ::averageWpm
                                )
                                .filter(Objects::nonNull)
                                .filter(value -> value > 0)
                                .mapToInt(Integer::intValue)
                                .average()
                                .orElse(0.0)
                );

        int totalFillerCount =
                fillerBreakdown.stream()
                        .mapToInt(
                                PresentationReportFillerBreakdownResponse::count
                        )
                        .sum();

        int silenceDetectedWindowCount =
                (int) windows.stream()
                        .filter(window ->
                                Boolean.TRUE.equals(
                                        window.silenceDetected()
                                )
                        )
                        .count();

        long totalSilenceDurationMs =
                windows.stream()
                        .map(
                                PresentationReportSpeechWindowResponse
                                        ::silenceDurationMs
                        )
                        .filter(Objects::nonNull)
                        .mapToLong(Long::longValue)
                        .sum();

        int stutterDetectedWindowCount =
                (int) windows.stream()
                        .filter(window ->
                                Boolean.TRUE.equals(
                                        window.stutterDetected()
                                )
                        )
                        .count();

        PresentationReportSpeechWindowSummaryResponse slowestWindow =
                windows.stream()
                        .filter(window ->
                                window.averageWpm() != null
                                && window.averageWpm() > 0
                        )
                        .min(
                                Comparator.comparingInt(
                                        PresentationReportSpeechWindowResponse
                                                ::averageWpm
                                )
                        )
                        .map(this::toSpeechWindowSummary)
                        .orElse(null);

        PresentationReportSpeechWindowSummaryResponse fastestWindow =
                windows.stream()
                        .filter(window ->
                                window.averageWpm() != null
                                && window.averageWpm() > 0
                        )
                        .max(
                                Comparator.comparingInt(
                                        PresentationReportSpeechWindowResponse
                                                ::averageWpm
                                )
                        )
                        .map(this::toSpeechWindowSummary)
                        .orElse(null);

        return new PresentationReportSpeechAnalysisResponse(
                averageWpm,
                totalFillerCount,
                fillerBreakdown,
                silenceDetectedWindowCount,
                totalSilenceDurationMs,
                stutterDetectedWindowCount,
                slowestWindow,
                fastestWindow,
                windows
        );
    }

    private List<PresentationReportSpeechWindowResponse> buildSpeechWindows(
            List<SpeechAnalysisLog> logs
    ) {
        return logs.stream()
                .map(log -> {
                    SpeechWindowMetadata metadata =
                            parseSpeechMetadata(
                                    log.getMetadata()
                            );

                    return new PresentationReportSpeechWindowResponse(
                            log.getId(),
                            log.getStartTimeMs(),
                            log.getEndTimeMs(),
                            metadata.averageWpm(),
                            metadata.fillerCount(),
                            buildFillerEventResponses(metadata.fillerEvents()),
                            metadata.silenceDetected(),
                            metadata.silenceDurationMs(),
                            metadata.stutterDetected(),
                            log.getDetailText()
                    );
                })
                .toList();
    }

    private List<PresentationReportSlideResponse> buildSlideResponses(
            Practice practice,
            Presentation presentation
    ) {
        TotalFeedback totalFeedback =
                totalFeedbackRepository
                        .findByPracticeId(practice.getId())
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.PRESENTATION_REPORT_NOT_FOUND
                                )
                        );

        List<PresentationSlide> slides =
                presentationSlideRepository
                        .findAllByPresentationIdOrderBySlideNumber(
                                presentation.getId()
                        );

        List<PresentationSlideFeedback> feedbacks =
                presentationSlideFeedbackRepository
                        .findAllByTotalFeedbackId(
                                totalFeedback.getId()
                        );

        Map<Long, PresentationSlideFeedback> feedbackBySlideId =
                feedbacks.stream()
                        .collect(
                                Collectors.toMap(
                                        PresentationSlideFeedback::getSlideId,
                                        Function.identity(),
                                        (existing, replacement) ->
                                                replacement
                                )
                        );

        return slides.stream()
                .map(slide -> {
                    PresentationSlideFeedback feedback =
                            feedbackBySlideId.get(
                                    slide.getId()
                            );

                    return new PresentationReportSlideResponse(
                            slide.getId(),
                            slide.getSlideNumber(),
                            s3SlideImageStorage.createReadUrl(
                                    slide.getImageKey()
                            ),
                            slide.getDescription() == null
                                    ? ""
                                    : slide.getDescription(),
                            slide.getStartTime(),
                            slide.getEndTime(),
                            buildSlideFeedbackResponse(feedback)
                    );
                })
                .toList();
    }

    private PresentationReportSlideFeedbackResponse buildSlideFeedbackResponse(
            PresentationSlideFeedback feedback
    ) {
        if (feedback == null) {
            return null;
        }

        return new PresentationReportSlideFeedbackResponse(
                feedback.getId(),
                feedback.getScore(),
                feedback.getContent()
        );
    }

    private List<PresentationReportQuestionAnswerResponse> buildQuestionAnswerResponses(
            Practice practice,
            Presentation presentation
    ) {
        List<PresentationQuestion> questions =
                presentationQuestionRepository
                        .findByPresentationIdOrderByIdAsc(
                                presentation.getId()
                        );

        if (questions.isEmpty()) {
            return List.of();
        }

        TotalFeedback totalFeedback =
                totalFeedbackRepository
                        .findByPracticeId(practice.getId())
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.PRESENTATION_REPORT_NOT_FOUND
                                )
                        );

        Map<Long, PresentationQuestionFeedback> feedbackByQuestionId =
                presentationQuestionFeedbackRepository
                        .findAllByTotalFeedbackId(
                                totalFeedback.getId()
                        )
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        PresentationQuestionFeedback::getQuestionId,
                                        Function.identity(),
                                        (existing, replacement) ->
                                                replacement
                                )
                        );

        return questions.stream()
                .map(question ->
                        new PresentationReportQuestionAnswerResponse(
                                question.getId(),
                                question.getQuestion(),
                                question.getModelAnswer(),
                                question.getUserAnswer(),
                                buildQuestionFeedbackResponse(
                                        feedbackByQuestionId.get(
                                                question.getId()
                                        )
                                )
                        )
                )
                .toList();
    }

    private PresentationReportQuestionFeedbackResponse buildQuestionFeedbackResponse(
            PresentationQuestionFeedback feedback
    ) {
        if (feedback == null) {
            return null;
        }

        return new PresentationReportQuestionFeedbackResponse(
                feedback.getId(),
                feedback.getScore(),
                feedback.getContent()
        );
    }

    private List<PresentationReportFillerEventResponse> buildFillerEventResponses(
            List<SpeechFillerEventMetadata> events
    ) {
        return events.stream()
                .map(event ->
                        new PresentationReportFillerEventResponse(
                                event.word(),
                                event.atSec()
                        )
                )
                .toList();
    }

    private SpeechWindowMetadata parseSpeechMetadata(
            String metadata
    ) {
        if (metadata == null || metadata.isBlank()) {
            return new SpeechWindowMetadata(
                    0,
                    0,
                    List.of(),
                    false,
                    0L,
                    false
            );
        }

        try {
            return normalizeSpeechMetadata(objectMapper.readValue(
                            metadata,
                            SpeechWindowMetadata.class
                    )
            );
        } catch (Exception e) {
            throw new CustomException(
                    ErrorCode.INVALID_SPEECH_ANALYSIS_METADATA
            );
        }
    }

    private List<PresentationReportFillerBreakdownResponse> buildFillerBreakdown(
            List<PresentationReportSpeechWindowResponse> windows
    ) {
        Map<String, Integer> countByWord =
                windows.stream()
                        .flatMap(window ->
                                window.fillerEvents().stream()
                        )
                        .map(
                                PresentationReportFillerEventResponse::word
                        )
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(word -> !word.isBlank())
                        .collect(
                                Collectors.groupingBy(
                                        Function.identity(),
                                        Collectors.summingInt(
                                                ignored -> 1
                                        )
                                )
                        );

        return countByWord.entrySet()
                .stream()
                .sorted(
                        Comparator
                                .<Map.Entry<String, Integer>>
                                        comparingInt(Map.Entry::getValue)
                                .reversed()
                                .thenComparing(Map.Entry::getKey)
                )
                .map(entry ->
                        new PresentationReportFillerBreakdownResponse(
                                entry.getKey(),
                                entry.getValue()
                        )
                )
                .toList();
    }


    private SpeechWindowMetadata normalizeSpeechMetadata(
            SpeechWindowMetadata metadata
    ) {
        return new SpeechWindowMetadata(
                metadata.averageWpm() == null
                        ? 0
                        : metadata.averageWpm(),

                metadata.fillerCount() == null
                        ? 0
                        : metadata.fillerCount(),

                metadata.fillerEvents() == null
                        ? List.of()
                        : metadata.fillerEvents(),

                Boolean.TRUE.equals(
                        metadata.silenceDetected()
                ),

                metadata.silenceDurationMs() == null
                        ? 0L
                        : metadata.silenceDurationMs(),

                Boolean.TRUE.equals(
                        metadata.stutterDetected()
                )
        );
    }

    private PresentationReportSpeechWindowSummaryResponse toSpeechWindowSummary(
            PresentationReportSpeechWindowResponse window
    ) {
        return new PresentationReportSpeechWindowSummaryResponse(
                window.startTimeMs(),
                window.endTimeMs(),
                window.averageWpm()
        );
    }

    private PresentationReportNonverbalAnalysisResponse buildNonverbalAnalysisResponse(
            Practice practice
    ) {
        Optional<NonverbalAnalysisLog> videoAnalysisLog =
                nonverbalAnalysisLogRepository
                        .findAllByPracticeIdOrderByCreatedAtAsc(
                                practice.getId()
                        )
                        .stream()
                        .filter(this::hasGestureMetadata)
                        .max(
                                Comparator
                                        .comparing(
                                                NonverbalAnalysisLog::getCreatedAt
                                        )
                                        .thenComparing(
                                                NonverbalAnalysisLog::getId
                                        )
                        );

        if (videoAnalysisLog.isEmpty()) {
            return emptyNonverbalAnalysisResponse();
        }

        NonverbalMetadata metadata =
                parseNonverbalMetadata(
                        videoAnalysisLog.get().getMetadata()
                );

        List<PresentationReportGestureBucketResponse> buckets =
                Optional.ofNullable(
                                metadata.tiltBuckets()
                        )
                        .orElseGet(List::of)
                        .stream()
                        .map(bucket ->
                                new PresentationReportGestureBucketResponse(
                                        bucket.startSec(),
                                        bucket.endSec(),
                                        bucket.tiltPct()
                                )
                        )
                        .toList();

        List<PresentationReportGazeEventResponse> gazeEvents =
                Optional.ofNullable(metadata.gazeEvents())
                        .orElseGet(List::of)
                        .stream()
                        .map(event ->
                                new PresentationReportGazeEventResponse(
                                        event.atSec()
                                )
                        )
                        .toList();

        int gazeCount =
                metadata.gazeDeviationCount() == null
                        ? gazeEvents.size()
                        : Math.max(
                        metadata.gazeDeviationCount(),
                        0
                );

        return new PresentationReportNonverbalAnalysisResponse(
                new PresentationReportGestureSeriesResponse(
                        buckets,
                        gazeCount,
                        gazeEvents
                )
        );
    }

    private PresentationReportNonverbalAnalysisResponse emptyNonverbalAnalysisResponse() {
        return new PresentationReportNonverbalAnalysisResponse(
                new PresentationReportGestureSeriesResponse(
                        List.of(),
                        0,
                        List.of()
                )
        );
    }

    private boolean hasGestureMetadata(
            NonverbalAnalysisLog log
    ) {
        String metadata = log.getMetadata();

        if (metadata == null || metadata.isBlank()) {
            return false;
        }

        return metadata.contains("\"tiltBuckets\"")
               && metadata.contains("\"gazeEvents\"");
    }

    private NonverbalMetadata parseNonverbalMetadata(
            String metadata
    ) {
        try {
            return objectMapper.readValue(
                    metadata,
                    NonverbalMetadata.class
            );
        } catch (Exception e) {
            throw new CustomException(
                    ErrorCode.INVALID_NONVERBAL_ANALYSIS_METADATA
            );
        }
    }

    private record NonverbalMetadata(
            Integer gazeDeviationCount,
            Double postureTiltPercent,
            Integer sampleCount,
            List<GazeEventMetadata> gazeEvents,
            List<TiltBucketMetadata> tiltBuckets
    ) {
    }

    private record GazeEventMetadata(
            Double atSec
    ) {
    }

    private record TiltBucketMetadata(
            Integer startSec,
            Integer endSec,
            Double tiltPct
    ) {
    }


    private record SpeechWindowMetadata(
            Integer averageWpm,
            Integer fillerCount,
            List<SpeechFillerEventMetadata> fillerEvents,
            Boolean silenceDetected,
            Long silenceDurationMs,
            Boolean stutterDetected
    ) {
    }

    private record SpeechFillerEventMetadata(
            String word,
            Integer atSec
    ) {
    }

}


