package com.ssafy.b109.aivo.presentation.dto;

public record PresentationReportSpeechWindowSummaryResponse(
        Long startTimeMs,
        Long endTimeMs,
        Integer averageWpm
) {
}