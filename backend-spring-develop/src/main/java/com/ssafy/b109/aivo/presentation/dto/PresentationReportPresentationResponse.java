package com.ssafy.b109.aivo.presentation.dto;

public record PresentationReportPresentationResponse(
        Long presentationId,
        Long targetDurationSec,
        Boolean aiQnaEnabled,
        long slideCount
) {
}
