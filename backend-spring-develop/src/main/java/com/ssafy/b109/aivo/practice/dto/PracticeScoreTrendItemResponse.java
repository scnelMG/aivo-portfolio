package com.ssafy.b109.aivo.practice.dto;

import java.time.LocalDateTime;

public record PracticeScoreTrendItemResponse(
        Long practiceId,
        LocalDateTime practicedAt,
        Short overallScore,
        Short voiceScore,
        Short videoScore,
        Short contentScore
) {
}