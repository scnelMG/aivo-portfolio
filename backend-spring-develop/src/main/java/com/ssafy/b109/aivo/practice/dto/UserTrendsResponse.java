package com.ssafy.b109.aivo.practice.dto;

import java.util.List;
import java.util.Map;

public record UserTrendsResponse(
        UserTrendMetricResponse earlyTrend,
        UserTrendMetricResponse lateTrend,
        List<UserPracticeScoreResponse> practices,
        UserSpeechTrendResponse speech
) {
}
