package com.ssafy.b109.aivo.presentation.dto;

import java.util.List;

public record PresentationReportResponse (
        PresentationReportPracticeResponse practice,
        PresentationReportPresentationResponse presentation,
        PresentationReportScoreResponse score,
        PresentationReportMediaResponse media,
        PresentationReportAudioSttResponse audioStt,
        PresentationReportSpeechAnalysisResponse speechAnalysis,
        PresentationReportNonverbalAnalysisResponse nonverbalAnalysis,
        List<PresentationReportSlideResponse> slides,
        List<PresentationReportQuestionAnswerResponse> questionAnswers
){
}
