package com.ssafy.b109.aivo.practice.repository.projection;

import java.time.LocalDateTime;

public interface PracticeArchiveStatisticsProjection {
    Long getFolderId();

    String getType();

    LocalDateTime getRecentPracticeDate();

    String getName();

    Short getAverageScore();

    String getDescription();

    Long getAttemptCount();

    Short getMaxScore();

    Short getRecentScore();
}
