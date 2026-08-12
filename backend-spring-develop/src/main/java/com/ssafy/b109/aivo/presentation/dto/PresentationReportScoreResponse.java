package com.ssafy.b109.aivo.presentation.dto;

public record PresentationReportScoreResponse(
        Short overallScore,
        Double folderAverageScore,
        Double folderAverageDelta,
        Short contentScore,
        Short voiceScore,
        Short videoScore,
        Short questionAnswerScore
) {
}
