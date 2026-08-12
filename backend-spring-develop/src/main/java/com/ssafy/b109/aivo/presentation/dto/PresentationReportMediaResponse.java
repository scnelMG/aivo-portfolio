package com.ssafy.b109.aivo.presentation.dto;

public record PresentationReportMediaResponse(
    PresentationReportVideoResponse video,
    PresentationReportAudioResponse audio
) {
}
