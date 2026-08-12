package com.ssafy.b109.aivo.practice.repository.projection;

import java.time.LocalDateTime;

public interface PracticeScoreTrendProjection {

    Long getPracticeId();

    LocalDateTime getPracticedAt();

    Short getOverallScore();

    Short getVoiceScore();

    Short getVideoScore();

    Short getContentScore();
}
