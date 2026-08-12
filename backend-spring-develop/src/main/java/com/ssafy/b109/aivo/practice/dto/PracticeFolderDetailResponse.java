package com.ssafy.b109.aivo.practice.dto;

public record PracticeFolderDetailResponse(
        Long folderId,
        String name,
        String description,
        Long attemptCount,
        Long totalDuration,
        Long maxScore
) {
}
