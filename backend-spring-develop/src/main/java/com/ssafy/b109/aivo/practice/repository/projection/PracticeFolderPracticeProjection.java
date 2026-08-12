package com.ssafy.b109.aivo.practice.repository.projection;

import java.time.LocalDateTime;

public interface PracticeFolderPracticeProjection {
    Long getPracticeId();

    Long getTargetId();

    String getTitle();

    Long getDurationSec();

    Short getOverallScore();

    LocalDateTime getCreatedAt();
}
