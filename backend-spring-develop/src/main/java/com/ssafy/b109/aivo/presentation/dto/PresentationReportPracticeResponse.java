package com.ssafy.b109.aivo.presentation.dto;

import java.time.LocalDateTime;

public record PresentationReportPracticeResponse(
        Long practiceId,
        String title,
        String description,
        LocalDateTime practicedAt,
        Long durationSec
) {
}
