package com.ssafy.b109.aivo.presentation.dto;

import java.util.List;

public record PresentationReportSpeechAnalysisResponse(
        Integer averageWpm,
        Integer totalFillerCount,
        List<PresentationReportFillerBreakdownResponse> fillerBreakdown,
        Integer silenceDetectedWindowCount,
        Long totalSilenceDurationMs,
        Integer stutterDetectedWindowCount,
        PresentationReportSpeechWindowSummaryResponse slowestWindow,
        PresentationReportSpeechWindowSummaryResponse fastestWindow,
        List<PresentationReportSpeechWindowResponse> windows
) {
}
