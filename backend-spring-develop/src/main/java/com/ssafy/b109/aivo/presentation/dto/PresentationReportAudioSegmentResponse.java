package com.ssafy.b109.aivo.presentation.dto;

public record PresentationReportAudioSegmentResponse(
        String text,
        Double timestampSt,
        Double timestampEnd,
        Long startTimeMs,
        Long endTimeMs,
        Long slideId
) {
}
