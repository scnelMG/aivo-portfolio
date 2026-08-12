package com.ssafy.b109.aivo.practice.repository.projection;

public interface PracticeFolderDetailStatisticsProjection {

    Long getAttemptCount();

    Long getTotalDuration();

    Long getMaxScore();
}
