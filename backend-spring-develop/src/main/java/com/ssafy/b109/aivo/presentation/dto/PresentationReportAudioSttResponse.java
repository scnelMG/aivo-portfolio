    package com.ssafy.b109.aivo.presentation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PresentationReportAudioSttResponse(
        Long audioSttId,
        Long audioId,
        String content,
        LocalDateTime createdAt,
        List<PresentationReportAudioSegmentResponse> segments
) {
}
