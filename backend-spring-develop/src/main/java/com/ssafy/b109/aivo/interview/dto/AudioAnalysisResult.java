package com.ssafy.b109.aivo.interview.dto;

import java.util.List;

public record AudioAnalysisResult(
        Integer fillerCount,
        Boolean silenceDetected,
        Boolean stutterDetected,
        Integer silenceDurationMs,
        Integer averageWpm,
        List<QuestionVoiceFillerEventResponse> fillerEvents,
        String feedback
) {
}
