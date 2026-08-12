package com.ssafy.b109.aivo.practice.dto;

import com.ssafy.b109.aivo.interview.dto.QuestionVoiceFillerEventResponse;

import java.util.List;

public record AudioAnalysisResponse(
        Long practiceId,
        Integer sequence,
        Integer fillerCount,
        Boolean silenceDetected,
        Boolean stutterDetected,
        Integer silenceDurationMs,
        Integer averageWpm,
        List<QuestionVoiceFillerEventResponse> fillerEvents,
        String feedback
) {
}
