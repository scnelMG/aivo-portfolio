package com.ssafy.b109.aivo.practice.dto;

public record UserPracticeScoreResponse(
        Integer contentScore,
        Integer videoScore,
        Integer voiceScore
) {
}
