package com.ssafy.b109.aivo.presentation.dto;

public record PresentationReportQuestionFeedbackResponse(
        Long feedbackId,
        Short score,
        String content
) {
}