package com.ssafy.b109.aivo.practice.service;

import com.ssafy.b109.aivo.nonverbal.entity.NonverbalAnalysisLog;
import com.ssafy.b109.aivo.nonverbal.repository.NonverbalAnalysisLogRepository;
import com.ssafy.b109.aivo.practice.dto.PracticeScoreResponse;
import com.ssafy.b109.aivo.speech.entity.SpeechAnalysisLog;
import com.ssafy.b109.aivo.speech.repository.SpeechAnalysisLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PracticeScoreService {
    private final SpeechAnalysisLogRepository speechAnalysisLogRepository;
    private final NonverbalAnalysisLogRepository nonverbalAnalysisLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();


    public PracticeScoreResponse calcPracticeScores(Long practiceId){
        Short voiceScore = calcVoiceScore(practiceId);
        Short videoScore = calcVideoScore(practiceId);
        return new PracticeScoreResponse(voiceScore, videoScore);
    }
    private Short calcVoiceScore(Long practiceId) {
        List<SpeechAnalysisLog> logs = speechAnalysisLogRepository.findByPracticeId(practiceId);
        // 100 - 필러(20) - 침묵(30) - 속도(50)

//        필러 : 100 음절당 필러어 횟수 (최대 20)
//        구하는 방식 : 한 단어에 (한글 기준) 대략 2.5~2.8 음절임. 빡빡하게 잡아서 2.5로 잡고
//        speech_analysis_log에서 totalTime = endTime - startTime (보통은 10초임) 로 시간 구하기
//        wpm = 60초간 발화한 단어 수 = 24초간 말한 음절 수
//        그럼 한 speech_analysis_log의 음절수는 wpm/24*totalTime 으로 계산 가능
//        그렇게 구한 모든 speech_analysis_log 들의 발화 음절 총합이 speechCount
//        모든 speech_analysis_log 들의 필러어 총합이 fillerCount
//        필러어 점수는 fillerCount/speechCount * 100 으로 책정

        int fillerScore = Math.min(20, calcFillerScore(logs));

//        침묵 : 침묵(1.5초 이상) 횟수 * 5 (최대 30)
//        침묵은 침묵 기간이 1500ms 이상인 구간이 있는 speech_analysis_log 마다 -5점
        int silenceScore = Math.min(30, calcSilenceScore(logs));


//        속도 : (최고 속도 - 최저 속도) / 평균 * 50 (최대 50)
//        평균 속도를 구하기 + 최고, 최저 속도 구하기  (이상치 제거)
//        (최대 - 최소)/평균 * 50 으로 점수 책정
        int wpmScore = Math.min(50, calcWpmScore(logs));



        return (short)(100 - fillerScore - silenceScore - wpmScore);
    }

    private int calcWpmScore(List<SpeechAnalysisLog> logs) {
        float maxWpm = 0;
        float minWpm = Float.MAX_VALUE;
        float sumWpm = 0;
        int wpmCount = 0;

        for(int i=0;i<logs.size();i++){
            SpeechAnalysisLog curLog = logs.get(i);
            float curWpm = curLog.getMetricValue();
            if(curWpm > 220) continue;
            if(curWpm<minWpm){
                minWpm = curWpm;
            }
            if(curWpm > maxWpm){
                maxWpm = curWpm;
            }
            sumWpm += curWpm;
            wpmCount++;
        }

        if(wpmCount == 0){
            return 0;
        }
        return (int)((maxWpm - minWpm)/(sumWpm/wpmCount) * 50);

    }

    private int calcSilenceScore(List<SpeechAnalysisLog> logs) {
        int silenceCount = 0;
        for(int i=0;i<logs.size();i++){
            SpeechAnalysisLog curLog = logs.get(i);
//            전체 필러어 횟수 세기
            try {
                JsonNode rootNode = objectMapper.readTree(curLog.getMetadata());
                Long silenceDurationMs = rootNode
                        .path("silenceDurationMs")
                        .asLong();

                if (silenceDurationMs == null || silenceDurationMs < 1500) {
                    continue;
                }
                silenceCount++;

            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "metadata JSON 파싱에 실패했습니다.",
                        e
                );
            }
        }

        return silenceCount*5;
    }

    private int calcFillerScore(List<SpeechAnalysisLog> logs) {
//        wpm/24*(endTime - startTime)/1000 의 총합
        double speechCount = 0;
        int fillerCount = 0;
        for(int i=0;i<logs.size();i++){
            SpeechAnalysisLog curLog = logs.get(i);
            speechCount += curLog.getMetricValue()/24*(curLog.getEndTimeMs() - curLog.getStartTimeMs())/1000;
//            전체 필러어 횟수 세기
            try {
                JsonNode rootNode = objectMapper.readTree(curLog.getMetadata());
                JsonNode fillerEventsNode = rootNode.get("fillerEvents");

                if (fillerEventsNode == null || !fillerEventsNode.isArray()) {
                    return 0;
                }

                fillerCount += (int)fillerEventsNode.findValues("atSec")
                        .stream()
                        .map(JsonNode::asInt)
                        .distinct()
                        .count();

            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "metadata JSON 파싱에 실패했습니다.",
                        e
                );
            }
        }
        return (int)(fillerCount / speechCount * 100);
    }

    private Short calcVideoScore(Long practiceId) {
        NonverbalAnalysisLog gazeLogs = nonverbalAnalysisLogRepository.findByPracticeIdAndEventType(practiceId, "VIDEO_GAZE_DEVIATION");
        NonverbalAnalysisLog tiltLogs = nonverbalAnalysisLogRepository.findByPracticeIdAndEventType(practiceId, "VIDEO_POSTURE_TILT");
//        100 - 시선(50) - 몸기울기(50)
//
//        시선 : 이탈 횟수 * 10 (최대 50점) -> 좀 가혹한것같은데? 분당 2회까지는 인정해줄까?
//        (분당 시선 이탈 횟수 - 2) * 5
        int eyeScore = Math.min(50, calcGazeScore(gazeLogs));
//
//        몸 기울기 : 퍼센트만큼 빼놓기

        int tiltScore = Math.min(50, calcTiltScore(tiltLogs));
//
//
//
        return (short)(100 - eyeScore - tiltScore);
    }

    private int calcTiltScore(NonverbalAnalysisLog tiltLogs) {
        float tiltPct = tiltLogs.getMetricValue();
        return (int)tiltPct;
    }

    private int calcGazeScore(NonverbalAnalysisLog gazeLogs) {
        float gazeCount = gazeLogs.getMetricValue();
        return (int)((gazeCount * 1000 / gazeLogs.getEndTimeMs() * 60 - 2) * 2);
    }
}
