package com.ssafy.b109.aivo.practice.util;

import com.ssafy.b109.aivo.feedback.entity.TotalFeedback;
import com.ssafy.b109.aivo.feedback.repository.TotalFeedbackRepository;
import com.ssafy.b109.aivo.interview.entity.InterviewScore;
import com.ssafy.b109.aivo.interview.repository.InterviewScoreRepository;
import com.ssafy.b109.aivo.nonverbal.entity.NonverbalAnalysisLog;
import com.ssafy.b109.aivo.nonverbal.repository.NonverbalAnalysisLogRepository;
import com.ssafy.b109.aivo.practice.dto.ContentScoreTrendResponse;
import com.ssafy.b109.aivo.practice.dto.FillerTrendResponse;
import com.ssafy.b109.aivo.practice.dto.GlanceTrendResponse;
import com.ssafy.b109.aivo.practice.dto.StabilityTrendResponse;
import com.ssafy.b109.aivo.practice.dto.UserSpeechTrendResponse;
import com.ssafy.b109.aivo.practice.dto.UserPracticeScoreResponse;
import com.ssafy.b109.aivo.practice.entity.Practice;
import com.ssafy.b109.aivo.practice.repository.PracticeRepository;
import com.ssafy.b109.aivo.practice.repository.projection.PracticeScoreTrendProjection;
import com.ssafy.b109.aivo.presentation.entity.PresentationScore;
import com.ssafy.b109.aivo.presentation.repository.PresentationScoreRepository;
import com.ssafy.b109.aivo.speech.repository.SpeechAnalysisLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PracticeScoreCalculator {

    private final PracticeRepository practiceRepository;
    private final InterviewScoreRepository interviewScoreRepository;
    private final PresentationScoreRepository presentationScoreRepository;
    private final TotalFeedbackRepository totalFeedbackRepository;
    private final NonverbalAnalysisLogRepository nonverbalAnalysisLogRepository;
    private final SpeechAnalysisLogRepository speechAnalysisLogRepository;

            /*
		"content" : 82, 내용 전달 점수
		"stability" : 71, 자세 안정도
		"glance" : 2.0, 시선이탈정도
		"filler" : 1.4, 필러 밀도
		"speed" : 11.2, 발화 속도 변동률
		"totalTime" : 5.2 목표 시간 오차
         */

    public ContentScoreTrendResponse calcContentScore(List<UserPracticeScoreResponse> practices
        , List<PracticeScoreTrendProjection> scoreTrends){
        // 앞에 절반이 옛날거 2개, 뒤에 절반이 최신 2개
        // 0 ~ 3 size = 4 ,

        int pivot = scoreTrends.size() / 2;


        Practice prac = null;
        // 1. interview인지 presentation인지 구분하고 옛날거 콘텐츠 점수 합산.
        int oldContentScore = 0;
        for(int i = 0 ; i < pivot; ++i){
            prac = practiceRepository.findById(scoreTrends.get(i).getPracticeId())
                    .orElseThrow(() -> new IllegalArgumentException("Custom 에러로 변경이 필요합니다."));

            if(prac.getPresentation() == null){
                // presentation이 null이라면 interview이다.
                TotalFeedback tf = totalFeedbackRepository.findByPracticeId(prac.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Custom 에러로 변경이 필요합니다."));
                InterviewScore is = interviewScoreRepository.findByTotalFeedbackId(tf.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Custom 에러로 변경이 필요합니다."));

                oldContentScore += is.getContentScore();
            } else {
                // presentation입니다.
                TotalFeedback tf = totalFeedbackRepository.findByPracticeId(prac.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Custom 에러로 변경이 필요합니다."));
                PresentationScore ps = presentationScoreRepository.findByTotalFeedbackId(tf.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Custom 에러로 변경이 필요합니다."));

                oldContentScore += ps.getContentScore();
            }
        }

        // 2. 최신 면접/발표 점수 계산
        int newContentScore = 0;
        for(int i = pivot ; i < scoreTrends.size(); ++i){

            prac = practiceRepository.findById(scoreTrends.get(i).getPracticeId())
                    .orElseThrow(() -> new IllegalArgumentException("Custom 에러로 변경이 필요합니다."));

            if(prac.getPresentation() == null){
                // presentation이 null이라면 interview이다.
                TotalFeedback tf = totalFeedbackRepository.findByPracticeId(prac.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Custom 에러로 변경이 필요합니다."));
                InterviewScore is = interviewScoreRepository.findByTotalFeedbackId(tf.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Custom 에러로 변경이 필요합니다."));

                newContentScore += is.getContentScore();
            } else {
                // presentation입니다.
                TotalFeedback tf = totalFeedbackRepository.findByPracticeId(prac.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Custom 에러로 변경이 필요합니다."));
                PresentationScore ps = presentationScoreRepository.findByTotalFeedbackId(tf.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Custom 에러로 변경이 필요합니다."));

                newContentScore += ps.getContentScore();
            }
        }

        ContentScoreTrendResponse res = new ContentScoreTrendResponse(
                oldContentScore / pivot,
                newContentScore / pivot
        );

        return res;
    }

    public GlanceTrendResponse calcGlanceScore(List<UserPracticeScoreResponse> practices
            , List<PracticeScoreTrendProjection> scoreTrends){
        int pivot = scoreTrends.size() / 2;

        Practice prac = null;

        float oldContentScore = 0.0f;
        for(int i = 0 ; i < pivot; ++i){
            prac = practiceRepository.findById(scoreTrends.get(i).getPracticeId())
                    .orElseThrow(() -> new IllegalArgumentException("Custom 에러로 변경이 필요합니다."));


            NonverbalAnalysisLog nLog = nonverbalAnalysisLogRepository.findGazeLogByPracticeId(prac.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Custom 에러로 변경이 필요합니다."));

            oldContentScore += (float)nLog.getMetricValue() / ( (float) nLog.getEndTimeMs() / 1000 / 60 );

        }

        // 2. 최신 면접/발표 점수 계산
        float newContentScore = 0.0f;
        for(int i = pivot ; i < scoreTrends.size(); ++i){

            prac = practiceRepository.findById(scoreTrends.get(i).getPracticeId())
                    .orElseThrow(() -> new IllegalArgumentException("Custom 에러로 변경이 필요합니다."));


            NonverbalAnalysisLog nLog = nonverbalAnalysisLogRepository.findGazeLogByPracticeId(prac.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Custom 에러로 변경이 필요합니다."));

            newContentScore += (float)nLog.getMetricValue() / ( (float) nLog.getEndTimeMs() / 1000 / 60 );

        }

        GlanceTrendResponse res = new GlanceTrendResponse(
                oldContentScore / pivot,
                newContentScore / pivot
        );

        return res;
    }

    public FillerTrendResponse calcFillerDensity(List<UserPracticeScoreResponse> practices,
                                                 List<PracticeScoreTrendProjection> scoreTrends) {
        int pivot = scoreTrends.size() / 2;

        double oldFillerCount = 0.0;
        for (int i = 0; i < pivot; ++i) {
            NonverbalAnalysisLog nLog = nonverbalAnalysisLogRepository.findGazeLogByPracticeId(scoreTrends.get(i).getPracticeId())
                    .orElseThrow(() -> new IllegalArgumentException("Custom Error로 변경이 필요합니다."));
            oldFillerCount += safeDouble(
                    speechAnalysisLogRepository.sumFillerCountByPracticeId(scoreTrends.get(i).getPracticeId())
            ) / ( nLog.getEndTimeMs() / 1000.0f );

        }

        double newFillerCount = 0.0;
        for (int i = pivot; i < scoreTrends.size(); ++i) {
            NonverbalAnalysisLog nLog = nonverbalAnalysisLogRepository.findGazeLogByPracticeId(scoreTrends.get(i).getPracticeId())
                    .orElseThrow(() -> new IllegalArgumentException("Custom Error로 변경이 필요합니다."));
            newFillerCount += safeDouble(
                    speechAnalysisLogRepository.sumFillerCountByPracticeId(scoreTrends.get(i).getPracticeId())
            )/ ( nLog.getEndTimeMs() / 1000.0f );
        }

        return new FillerTrendResponse(
                roundOne(average(oldFillerCount, pivot)),
                roundOne(average(newFillerCount, scoreTrends.size() - pivot))
        );
    }

    public UserSpeechTrendResponse calcSpeechSpeed(List<UserPracticeScoreResponse> practices,
                                                   List<PracticeScoreTrendProjection> scoreTrends) {
        int pivot = scoreTrends.size() / 2;

        double oldSpeedChangePercent = 0.0;
        for (int i = 0; i < pivot; ++i) {
            oldSpeedChangePercent += safeDouble(
                    speechAnalysisLogRepository.speedChangePercentByPracticeId(scoreTrends.get(i).getPracticeId())
            );
        }

        double newSpeedChangePercent = 0.0;
        for (int i = pivot; i < scoreTrends.size(); ++i) {
            newSpeedChangePercent += safeDouble(
                    speechAnalysisLogRepository.speedChangePercentByPracticeId(scoreTrends.get(i).getPracticeId())
            );
        }

        int earlySpeechSpeed = roundInt(average(oldSpeedChangePercent, pivot));
        int lateSpeechSpeed = roundInt(average(newSpeedChangePercent, scoreTrends.size() - pivot));
        int averageSpeechSpeed = roundInt(average(oldSpeedChangePercent + newSpeedChangePercent, scoreTrends.size()));

        return new UserSpeechTrendResponse(
                averageSpeechSpeed,
                earlySpeechSpeed,
                lateSpeechSpeed,
                null
        );
    }

    public StabilityTrendResponse calcPostureStability(List<UserPracticeScoreResponse> practices,
                                                       List<PracticeScoreTrendProjection> scoreTrends) {
        int pivot = scoreTrends.size() / 2;

        double oldStabilityScore = 0.0;
        for (int i = 0; i < pivot; ++i) {
            oldStabilityScore += safeDouble(
                    nonverbalAnalysisLogRepository.postureStabilityScoreByPracticeId(scoreTrends.get(i).getPracticeId())
            );
        }

        double newStabilityScore = 0.0;
        for (int i = pivot; i < scoreTrends.size(); ++i) {
            newStabilityScore += safeDouble(
                    nonverbalAnalysisLogRepository.postureStabilityScoreByPracticeId(scoreTrends.get(i).getPracticeId())
            );
        }

        return new StabilityTrendResponse(
                roundInt(average(oldStabilityScore, pivot)),
                roundInt(average(newStabilityScore, scoreTrends.size() - pivot))
        );
    }

    public Double[] calcTotalTime(List<UserPracticeScoreResponse> practices,
                                  List<PracticeScoreTrendProjection> scoreTrends) {
        int pivot = scoreTrends.size() / 2;

        double oldTotalTime = 0.0;
        for (int i = 0; i < pivot; ++i) {
            Practice prac = practiceRepository.findById(scoreTrends.get(i).getPracticeId())
                    .orElseThrow(() -> new IllegalArgumentException("Custom Error濡?蹂寃쎌씠 ?꾩슂?⑸땲??"));
            oldTotalTime += safeLong(prac.getDurationSec());
        }

        double newTotalTime = 0.0;
        for (int i = pivot; i < scoreTrends.size(); ++i) {
            Practice prac = practiceRepository.findById(scoreTrends.get(i).getPracticeId())
                    .orElseThrow(() -> new IllegalArgumentException("Custom Error濡?蹂寃쎌씠 ?꾩슂?⑸땲??"));
            newTotalTime += safeLong(prac.getDurationSec());
        }

        return new Double[] {
                roundOne(average(oldTotalTime, pivot)),
                roundOne(average(newTotalTime, scoreTrends.size() - pivot))
        };
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private double safeLong(Long value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private double average(double total, int count) {
        return count <= 0 ? 0.0 : total / count;
    }

    private double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private int roundInt(double value) {
        return (int) Math.round(value);
    }
}
