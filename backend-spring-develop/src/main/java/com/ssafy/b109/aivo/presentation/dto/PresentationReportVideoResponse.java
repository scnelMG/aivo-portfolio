package com.ssafy.b109.aivo.presentation.dto;

public record PresentationReportVideoResponse(
        Long videoId,
        String contentType,
        Long size,
        String playbackUrl
) {
}
