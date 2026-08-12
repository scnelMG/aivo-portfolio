package com.ssafy.b109.aivo.interview.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AudioSttSegmentResponse(
        Double start,
        Double end,
        Long startTimeMs,
        Long endTimeMs,
        String text
) {
}
