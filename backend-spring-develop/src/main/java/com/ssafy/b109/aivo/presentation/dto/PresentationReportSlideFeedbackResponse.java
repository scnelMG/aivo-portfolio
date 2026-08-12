package com.ssafy.b109.aivo.presentation.dto;

public record PresentationReportSlideFeedbackResponse(
        Long feedbackId,
        Short score,
        String content
) {
}
