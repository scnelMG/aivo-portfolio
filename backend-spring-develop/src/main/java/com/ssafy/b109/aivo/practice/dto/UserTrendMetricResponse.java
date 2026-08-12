package com.ssafy.b109.aivo.practice.dto;

public record UserTrendMetricResponse(
        Integer content,
        Integer stability,
        Double glance,
        Double filler,
        Double speed,
        Double totalTime
) {
}
