package com.ssafy.b109.aivo.interview.dto;

public record NonverbalSummaryResponse(
        Integer analyzedChunks,
        Integer totalFillerCount,
        Integer silenceCount,
        Integer stutterCount,
        Integer averageWpm,
        Integer minWpm,
        Integer maxWpm,
        String feedback
) {
}
