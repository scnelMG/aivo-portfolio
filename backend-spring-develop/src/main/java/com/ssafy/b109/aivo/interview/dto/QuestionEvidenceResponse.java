package com.ssafy.b109.aivo.interview.dto;

public record QuestionEvidenceResponse(
        String type,
        String text,
        Integer startIndex,
        Integer endIndex,
        String reason
) {
}
