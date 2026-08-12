package com.ssafy.b109.aivo.practice.dto;

import java.util.List;

public record PracticeScoreTrendResponse(
        List<PracticeScoreTrendItemResponse> scores
) {
}
