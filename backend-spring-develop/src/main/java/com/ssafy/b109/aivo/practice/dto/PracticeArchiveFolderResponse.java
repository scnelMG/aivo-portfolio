package com.ssafy.b109.aivo.practice.dto;

import java.time.LocalDateTime;

public record PracticeArchiveFolderResponse(
        Long folderId,
        String type,
        LocalDateTime recentPracticeDate,
        String name,
        Short averageScore,
        String description,
        Long attemptCount,
        Short maxScore,
        Short recentScore
) {

}
