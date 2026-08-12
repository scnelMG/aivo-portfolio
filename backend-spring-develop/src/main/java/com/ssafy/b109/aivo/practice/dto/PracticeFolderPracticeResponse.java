package com.ssafy.b109.aivo.practice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PracticeFolderPracticeResponse(
        Long practiceId,
        Long presentationId,
        Long interviewId,
        String title,
        String type,
        Long durationSec,
        Short overallScore,
        LocalDateTime createdAt
) {
}
