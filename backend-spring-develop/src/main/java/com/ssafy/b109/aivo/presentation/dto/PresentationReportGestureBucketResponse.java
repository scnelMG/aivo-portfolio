package com.ssafy.b109.aivo.presentation.dto;

public record PresentationReportGestureBucketResponse(
        Integer startSec,
        Integer endSec,
        Double tiltPct
) {
}
