package com.ssafy.b109.aivo.presentation.dto;

public record PresentationReportQuestionAnswerResponse(
        Long questionId,
        String question,
        String modelAnswer,
        String userAnswer,
        PresentationReportQuestionFeedbackResponse feedback
) {
}