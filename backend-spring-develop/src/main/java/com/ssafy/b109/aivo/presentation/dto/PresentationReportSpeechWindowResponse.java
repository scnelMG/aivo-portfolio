package com.ssafy.b109.aivo.presentation.dto;

import java.util.List;

public record PresentationReportSpeechWindowResponse(
        Long logId,
        Long startTimeMs,
        Long endTimeMs,
        Integer averageWpm,
        Integer fillerCount,
        List<PresentationReportFillerEventResponse> fillerEvents,
        Boolean silenceDetected,
        Long silenceDurationMs,
        Boolean stutterDetected,
        String feedback
) {
}