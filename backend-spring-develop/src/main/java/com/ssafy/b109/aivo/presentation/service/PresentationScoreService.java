package com.ssafy.b109.aivo.presentation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.b109.aivo.feedback.entity.TotalFeedback;
import com.ssafy.b109.aivo.feedback.repository.TotalFeedbackRepository;
import com.ssafy.b109.aivo.global.exception.CustomException;
import com.ssafy.b109.aivo.global.exception.ErrorCode;
import com.ssafy.b109.aivo.nonverbal.entity.NonverbalAnalysisLog;
import com.ssafy.b109.aivo.nonverbal.repository.NonverbalAnalysisLogRepository;
import com.ssafy.b109.aivo.practice.dto.PracticeScoreResponse;
import com.ssafy.b109.aivo.practice.entity.Practice;
import com.ssafy.b109.aivo.practice.repository.PracticeRepository;
import com.ssafy.b109.aivo.practice.service.PracticeScoreService;
import com.ssafy.b109.aivo.presentation.entity.Presentation;
import com.ssafy.b109.aivo.presentation.entity.PresentationScore;
import com.ssafy.b109.aivo.presentation.entity.PresentationSlide;
import com.ssafy.b109.aivo.presentation.entity.PresentationSlideFeedback;
import com.ssafy.b109.aivo.presentation.repository.PresentationScoreRepository;
import com.ssafy.b109.aivo.presentation.repository.PresentationSlideFeedbackRepository;
import com.ssafy.b109.aivo.presentation.repository.PresentationSlideRepository;
import com.ssafy.b109.aivo.speech.entity.SpeechAnalysisLog;
import com.ssafy.b109.aivo.speech.repository.SpeechAnalysisLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresentationScoreService {

    private static final int BASE_SCORE = 100;
    private static final double VOICE_SCORE_WEIGHT = 0.30;
    private static final double VIDEO_SCORE_WEIGHT = 0.25;
    private static final double CONTENT_SCORE_WEIGHT = 0.45;


    private final PracticeRepository practiceRepository;
    private final TotalFeedbackRepository totalFeedbackRepository;
    private final PresentationScoreRepository presentationScoreRepository;
    private final PresentationSlideRepository presentationSlideRepository;
    private final PresentationSlideFeedbackRepository presentationSlideFeedbackRepository;
    private final PracticeScoreService practiceScoreService;

    @Transactional
    public void setPresentationScore(Long practiceId) {
        Practice practice = practiceRepository.findById(practiceId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND_PRACTICE
                ));

        Presentation presentation = practice.getPresentation();
        if (presentation == null) {
            log.info(
                    "발표 practice가 아니므로 발표 점수 생성을 건너뜁니다: practiceId={}",
                    practiceId
            );
            return;
        }

        TotalFeedback totalFeedback = totalFeedbackRepository.findByPracticeId(practiceId)
                .orElseThrow(() -> new RuntimeException("total_feedback을 찾을 수 없습니다."));
        Long totalFeedbackId = totalFeedback.getId();
        Short contentScore = calculateContentScore(presentation.getId());
        PracticeScoreResponse practiceScoreResponse = practiceScoreService.calcPracticeScores(practiceId);
        Short videoScore = practiceScoreResponse.videoScore();
        Short voiceScore = practiceScoreResponse.voiceScore();
        Short overallScore = calculateOverallScore(voiceScore, videoScore, contentScore);

        PresentationScore presentationScore = presentationScoreRepository
                .findByTotalFeedbackId(totalFeedbackId)
                .orElseGet(PresentationScore::new);

        presentationScore.setTotalFeedbackId(totalFeedbackId);
        presentationScore.setContentScore(contentScore);
        presentationScore.setVideoScore(videoScore);
        presentationScore.setVoiceScore(voiceScore);
        presentationScore.setOverallScore(overallScore);
        presentationScore.setCreatedAt(LocalDateTime.now());

        presentationScoreRepository.save(presentationScore);
    }


    private Short calculateContentScore(Long presentationId) {
        List<PresentationSlide> slides =
                presentationSlideRepository.findAllByPresentationIdOrderBySlideNumber(presentationId);

        double weightedScoreSum = 0;
        double totalDuration = 0;

        for (PresentationSlide slide : slides) {
            Float startTime = slide.getStartTime();
            Float endTime = slide.getEndTime();

            if (startTime == null || endTime == null || endTime <= startTime) {
                continue;
            }

            PresentationSlideFeedback feedback = presentationSlideFeedbackRepository
                    .findBySlideId(slide.getId())
                    .orElse(null);

            if (feedback == null || feedback.getScore() == null) {
                continue;
            }

            double duration = endTime - startTime;
            weightedScoreSum += feedback.getScore() * duration;
            totalDuration += duration;
        }

        if (totalDuration == 0) {
            return 0;
        }

        return toScore(weightedScoreSum / totalDuration);
    }

    private Short toScore(double score) {
        int roundedScore = (int) Math.round(score);
        int clampedScore = Math.max(0, Math.min(BASE_SCORE, roundedScore));
        return (short) clampedScore;
    }

    private Short calculateOverallScore(
            Short voiceScore,
            Short videoScore,
            Short contentScore
    ) {
        double overallScore =
                voiceScore * VOICE_SCORE_WEIGHT +
                        videoScore * VIDEO_SCORE_WEIGHT +
                        contentScore * CONTENT_SCORE_WEIGHT;
        return toScore(overallScore);
    }
}
