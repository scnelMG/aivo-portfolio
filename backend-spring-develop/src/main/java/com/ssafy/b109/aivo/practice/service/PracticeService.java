package com.ssafy.b109.aivo.practice.service;

import com.ssafy.b109.aivo.feedback.entity.TotalFeedback;
import com.ssafy.b109.aivo.feedback.repository.TotalFeedbackRepository;
import com.ssafy.b109.aivo.global.exception.CustomException;
import com.ssafy.b109.aivo.global.exception.ErrorCode;
import com.ssafy.b109.aivo.practice.dto.*;
import com.ssafy.b109.aivo.practice.entity.Practice;
import com.ssafy.b109.aivo.practice.entity.PracticeFolder;
import com.ssafy.b109.aivo.practice.repository.PracticeFolderRepository;
import com.ssafy.b109.aivo.practice.repository.PracticeRepository;
import com.ssafy.b109.aivo.practice.repository.projection.PracticeScoreTrendProjection;
import com.ssafy.b109.aivo.practice.util.PracticeScoreCalculator;
import com.ssafy.b109.aivo.presentation.entity.Presentation;
import com.ssafy.b109.aivo.presentation.entity.PresentationProcessingStatus;
import com.ssafy.b109.aivo.speech.repository.SpeechAnalysisLogRepository;
import com.ssafy.b109.aivo.speech.repository.projection.SpeechSpeedSectionAverageProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PracticeService {

    private final PracticeRepository practiceRepository;
    private final PracticeFolderRepository practiceFolderRepository;
    private final PracticeAudioService practiceAudioService;
    private final TotalFeedbackRepository totalFeedbackRepository;
    private final PracticeScoreCalculator practiceScoreCalculator;
    private final SpeechAnalysisLogRepository speechAnalysisLogRepository;

    @Transactional(readOnly = true)
    public UserTrendsResponse getPracticesScoreTrends(Long userId) {
        Long count = practiceRepository.countAllByUserId(userId);
        int window;
        if(count >= 6){
            window = 6;
        }else if(count >= 4){
            window = 4;
        }else if(count >= 2){
            window = 2;
        }else{
            return null;
        }
        List<PracticeScoreTrendProjection> scoreTrends =
                practiceRepository.findRecentScoreTrendsByUserId(userId, window);

        List<UserPracticeScoreResponse> practices =
                scoreTrends.stream()
                        .map(score -> new UserPracticeScoreResponse(
                                toInteger(score.getContentScore()),
                                toInteger(score.getVideoScore()),
                                toInteger(score.getVoiceScore())
                        ))
                        .toList();
//        UserTrendMetricResponse earlyTrend = new UserTrendMetricResponse();
//        UserTrendMetricResponse lateTrend = new UserTrendMetricResponse();
        Integer averageSpeechSpeed = toRoundedInteger(
                speechAnalysisLogRepository.findAverageMetricValueByUserId(userId)
        );
        SpeechSpeedSectionAverageProjection speechSpeedSections =
                speechAnalysisLogRepository.findSpeechSpeedSectionAveragesByUserId(userId);
        Integer earlySpeechSpeed = speechSpeedSections == null
                ? null
                : toRoundedInteger(speechSpeedSections.getEarlySpeechSpeed());
        Integer lateSpeechSpeed = speechSpeedSections == null
                ? null
                : toRoundedInteger(speechSpeedSections.getLateSpeechSpeed());
        Double silenceLate =
                speechAnalysisLogRepository.findSilenceDurationRatioByUserId(userId);

        UserSpeechTrendResponse speech = new UserSpeechTrendResponse(
                averageSpeechSpeed,
                earlySpeechSpeed,
                lateSpeechSpeed,
                silenceLate
        );
//        UserTrendsResponse userTrendsResponse = new UserTrendsResponse();
        ContentScoreTrendResponse contentScores = practiceScoreCalculator.calcContentScore(practices, scoreTrends);
        GlanceTrendResponse glanceTrendResponse = practiceScoreCalculator.calcGlanceScore(practices, scoreTrends);
        FillerTrendResponse fillerTrendResponse = practiceScoreCalculator.calcFillerDensity(practices, scoreTrends);
        UserSpeechTrendResponse speechTrendResponse = practiceScoreCalculator.calcSpeechSpeed(practices, scoreTrends);
        StabilityTrendResponse stabilityTrendResponse = practiceScoreCalculator.calcPostureStability(practices, scoreTrends);
        Double[] totalTimeTrend = practiceScoreCalculator.calcTotalTime(practices, scoreTrends);
        return new UserTrendsResponse(
                new UserTrendMetricResponse(
                        contentScores.oldScore(),
                        stabilityTrendResponse.oldScore(),
                        (double) glanceTrendResponse.oldCount(),
                        fillerTrendResponse.oldDensity(),
                        toDouble(speechTrendResponse.earlySpeechSpeed()),
                        totalTimeTrend[0]
                ),
                new UserTrendMetricResponse(
                        contentScores.newScore(),
                        stabilityTrendResponse.newScore(),
                        (double) glanceTrendResponse.newCount(),
                        fillerTrendResponse.newDensity(),
                        toDouble(speechTrendResponse.lateSpeechSpeed()),
                        totalTimeTrend[1]
                ),
                practices,
                speech
        );
    }

    private Double toDouble(Integer value) {
        return value == null ? null : value.doubleValue();
    }

    private Integer toInteger(Short score) {
        return score == null ? null : score.intValue();
    }

    private Integer toRoundedInteger(Double value) {
        return value == null ? null : (int) Math.round(value);
    }

    @Transactional
    public Practice createForPresentation(
            PracticeFolder folder,
            Presentation presentation,
            String title,
            String description
    ) {
        Practice practice = new Practice();

        practice.setFolder(folder);
        practice.setPresentation(presentation);
        practice.setTitle(title);
        practice.setDescription(description);
        practice.setDurationSec(0L);
        practice.setCreatedAt(LocalDateTime.now());
        practice.setUpdatedAt(LocalDateTime.now());

        Practice savedPractice = practiceRepository.save(practice);
        createTotalFeedback(savedPractice.getId());
        return savedPractice;
    }

    private void createTotalFeedback(Long practiceId) {
        TotalFeedback totalFeedback = new TotalFeedback();
        totalFeedback.setPracticeId(practiceId);
        totalFeedback.setNonverbalFeedback("{}");
        totalFeedback.setSpeechSpeed(0L);
        totalFeedbackRepository.save(totalFeedback);
    }

    public void validatePresentationOwner(Long userId, Long presentationId) {
        boolean owned = practiceRepository.existsOwnedPresentation(presentationId, userId);

        if(!owned) {
            throw new CustomException(
                    ErrorCode.PRESENTATION_NOT_FOUND
            );
        }
    }

    @Transactional
    public AudioAnalysisResponse analyzeAudio(
            Long practiceId,
            MultipartFile audioFile,
            Integer sequence,
            Long userId
    ) {
        Practice practice = getAuthorizedPractice(practiceId, userId);
        return practiceAudioService.analyzeChunk(practice, audioFile, sequence);
    }

    @Transactional
    public AudioAnalysisResponse analyzeInterviewAudio(
            Long interviewId,
            MultipartFile audioFile,
            Integer sequence,
            Long userId
    ) {
        Practice practice = getAuthorizedInterviewPractice(interviewId, userId);
        return practiceAudioService.analyzeChunk(practice, audioFile, sequence);
    }

    private Practice getAuthorizedPractice(Long practiceId, Long userId) {
        if (practiceId == null) {
            throw new CustomException(ErrorCode.NOT_FOUND_PRACTICE);
        }

        return practiceRepository.findByIdAndFolder_User_Id(practiceId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_PRACTICE));
    }

    private Practice getAuthorizedInterviewPractice(Long interviewId, Long userId) {
        if (interviewId == null) {
            throw new CustomException(ErrorCode.NOT_FOUND_PRACTICE);
        }

        return practiceRepository.findByInterviewSession_IdAndFolder_User_Id(interviewId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_PRACTICE));
    }

    @Transactional(readOnly = true)
    public PresentationPracticeListResponse
    getPresentationPractices(
            Long userId,
            Long folderId
    ) {
        practiceFolderRepository
                .findByIdAndUserId(
                        folderId,
                        userId
                )
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.NOT_FOUND_PRACTICE_FOLDER
                        )
                );

        List<Practice> practices =
                practiceRepository
                        .findReusablePresentationPractices(
                                folderId,
                                userId,
                                PresentationProcessingStatus.COMPLETED
                        );

        List<PresentationPracticeListItemResponse> items =
                practices.stream()
                        .map(practice ->
                                new PresentationPracticeListItemResponse(
                                        practice.getId(),
                                        practice.getPresentation().getId(),
                                        practice.getTitle(),
                                        practice.getDescription(),
                                        practice.getDurationSec(),
                                        practice.getCreatedAt()
                                )
                        )
                        .toList();

        return new PresentationPracticeListResponse(
                items
        );
    }

}
