package com.ssafy.b109.aivo.interview.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.b109.aivo.global.exception.CustomException;
import com.ssafy.b109.aivo.global.exception.ErrorCode;
import com.ssafy.b109.aivo.interview.dto.AudioSttSegmentResponse;
import com.ssafy.b109.aivo.interview.dto.FullAudioTranscriptionResult;
import com.ssafy.b109.aivo.interview.dto.InterviewNonverbalRequest;
import com.ssafy.b109.aivo.media.entity.Audio;
import com.ssafy.b109.aivo.media.entity.AudioSegment;
import com.ssafy.b109.aivo.media.entity.AudioStt;
import com.ssafy.b109.aivo.media.entity.MediaDomain;
import com.ssafy.b109.aivo.media.entity.Video;
import com.ssafy.b109.aivo.media.repository.AudioSegmentRepository;
import com.ssafy.b109.aivo.media.repository.AudioSttRepository;
import com.ssafy.b109.aivo.media.service.MediaService;
import com.ssafy.b109.aivo.nonverbal.entity.NonverbalAnalysisLog;
import com.ssafy.b109.aivo.nonverbal.repository.NonverbalAnalysisLogRepository;
import com.ssafy.b109.aivo.practice.entity.Practice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class InterviewAudioService {

    private static final String VIDEO_GAZE_DEVIATION = "VIDEO_GAZE_DEVIATION";
    private static final String VIDEO_POSTURE_TILT = "VIDEO_POSTURE_TILT";
    private static final List<String> VIDEO_SUMMARY_EVENT_TYPES = List.of(VIDEO_GAZE_DEVIATION, VIDEO_POSTURE_TILT);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final FastApiAudioAnalysisClient fastApiAudioAnalysisClient;
    private final AudioSttRepository audioSttRepository;
    private final AudioSegmentRepository audioSegmentRepository;
    private final MediaService mediaService;
    private final NonverbalAnalysisLogRepository nonverbalAnalysisLogRepository;

    public FullAudioTranscriptionResult saveFullAudioAndTranscribe(
            Long interviewId,
            Practice practice,
            MultipartFile fullAudioFile,
            Long userId
    ) {
        validateAudioFile(fullAudioFile);

        Audio savedAudio = mediaService.uploadAudio(
                userId,
                practice,
                MediaDomain.INTERVIEW,
                interviewId,
                fullAudioFile
        );
        FullAudioTranscriptionResult result = fastApiAudioAnalysisClient.transcribeFullAudio(interviewId, fullAudioFile);
        String transcript = result.transcript();
        if (transcript == null || transcript.isBlank()) {
            return FullAudioTranscriptionResult.empty();
        }

        AudioStt audioStt = audioSttRepository.findFirstByAudioIdOrderByIdDesc(savedAudio.getId())
                .orElseGet(AudioStt::new);
        if (audioStt.getAudio() == null) {
            audioStt.setAudio(savedAudio);
        }
        audioStt.setContent(transcript);
        audioStt.setCreatedAt(LocalDateTime.now());
        audioSttRepository.saveAndFlush(audioStt);
        saveAudioSegments(savedAudio, result.segments());
        return result;
    }

    public Audio saveFullAudio(
            Long interviewId,
            Practice practice,
            MultipartFile fullAudioFile,
            Long userId
    ) {
        validateAudioFile(fullAudioFile);

        return mediaService.uploadAudio(
                userId,
                practice,
                MediaDomain.INTERVIEW,
                interviewId,
                fullAudioFile
        );
    }

    private void saveAudioSegments(Audio audio, List<AudioSttSegmentResponse> segments) {
        audioSegmentRepository.deleteAllByAudio_Id(audio.getId());
        if (segments == null || segments.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        audioSegmentRepository.saveAll(IntStream.range(0, segments.size())
                .mapToObj(index -> toAudioSegment(audio, segments.get(index), index, now))
                .toList());
    }

    private AudioSegment toAudioSegment(
            Audio audio,
            AudioSttSegmentResponse segment,
            int sequence,
            LocalDateTime createdAt
    ) {
        AudioSegment audioSegment = new AudioSegment();
        audioSegment.setAudio(audio);
        audioSegment.setSequence(sequence);
        audioSegment.setStartSec(segmentStartSec(segment));
        audioSegment.setEndSec(segmentEndSec(segment));
        audioSegment.setStartTimeMs(segmentStartMs(segment));
        audioSegment.setEndTimeMs(segmentEndMs(segment));
        audioSegment.setText(segment.text() == null ? "" : segment.text());
        audioSegment.setCreatedAt(createdAt);
        return audioSegment;
    }

    private double segmentStartSec(AudioSttSegmentResponse segment) {
        if (segment.start() != null) {
            return segment.start();
        }
        return segmentStartMs(segment) / 1000.0;
    }

    private double segmentEndSec(AudioSttSegmentResponse segment) {
        if (segment.end() != null) {
            return segment.end();
        }
        return segmentEndMs(segment) / 1000.0;
    }

    private long segmentStartMs(AudioSttSegmentResponse segment) {
        if (segment.startTimeMs() != null) {
            return segment.startTimeMs();
        }
        return Math.round(safeDouble(segment.start()) * 1000);
    }

    private long segmentEndMs(AudioSttSegmentResponse segment) {
        if (segment.endTimeMs() != null) {
            return segment.endTimeMs();
        }
        return Math.round(safeDouble(segment.end()) * 1000);
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    public FullAudioTranscriptionResult transcribeFullAudioForTest(MultipartFile fullAudioFile) {
        validateAudioFile(fullAudioFile);
        return fastApiAudioAnalysisClient.transcribeFullAudio(0L, fullAudioFile);
    }

    public Video saveFullVideo(
            Long interviewId,
            Practice practice,
            MultipartFile fullVideoFile,
            Long userId
    ) {
        validateMediaFile(fullVideoFile);

        return mediaService.uploadVideo(
                userId,
                practice,
                MediaDomain.INTERVIEW,
                interviewId,
                fullVideoFile
        );
    }

    public void saveVideoNonverbalSummary(
            Practice practice,
            InterviewNonverbalRequest nonverbal,
            Long durationSec
    ) {
        if (practice == null || nonverbal == null || !hasVideoMetric(nonverbal)) {
            return;
        }

        nonverbalAnalysisLogRepository.deleteByPracticeIdAndEventTypeIn(
                practice.getId(),
                VIDEO_SUMMARY_EVENT_TYPES
        );

        String metadata = toMetadata(nonverbal);
        Long endTimeMs = durationSec == null ? practice.getDurationSec() : durationSec;
        endTimeMs = endTimeMs == null ? null : endTimeMs * 1000L;

        nonverbalAnalysisLogRepository.save(buildVideoLog(
                practice.getId(),
                VIDEO_GAZE_DEVIATION,
                nonverbal.gazeDeviationCount(),
                metadata,
                endTimeMs
        ));
        nonverbalAnalysisLogRepository.save(buildVideoLog(
                practice.getId(),
                VIDEO_POSTURE_TILT,
                nonverbal.postureTiltPercent(),
                metadata,
                endTimeMs
        ));
    }

    private NonverbalAnalysisLog buildVideoLog(
            Long practiceId,
            String eventType,
            Integer metricValue,
            String metadata,
            Long endTimeMs
    ) {
        NonverbalAnalysisLog log = new NonverbalAnalysisLog();
        log.setPracticeId(practiceId);
        log.setStartTimeMs(0L);
        log.setEndTimeMs(endTimeMs);
        log.setEventType(eventType);
        log.setMetricValue(metricValue == null ? 0F : metricValue.floatValue());
        log.setMetadata(metadata);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }

    private String toMetadata(InterviewNonverbalRequest nonverbal) {
        try {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("gazeDeviationCount", safeInt(nonverbal.gazeDeviationCount()));
            metadata.put("postureTiltPercent", safeInt(nonverbal.postureTiltPercent()));
            metadata.put("sampleCount", safeInt(nonverbal.sampleCount()));
            metadata.put("gazeEvents", nonverbal.gazeEvents() == null ? List.of() : nonverbal.gazeEvents());
            metadata.put("tiltBuckets", nonverbal.tiltBuckets() == null ? List.of() : nonverbal.tiltBuckets());
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private void validateAudioFile(MultipartFile audioFile) {
        validateMediaFile(audioFile);
    }

    private void validateMediaFile(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_AUDIO_ANALYSIS_REQUEST);
        }
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean hasVideoMetric(InterviewNonverbalRequest nonverbal) {
        return nonverbal.gazeDeviationCount() != null
                || nonverbal.postureTiltPercent() != null
                || (nonverbal.gazeEvents() != null && !nonverbal.gazeEvents().isEmpty())
                || (nonverbal.tiltBuckets() != null && !nonverbal.tiltBuckets().isEmpty());
    }
}
