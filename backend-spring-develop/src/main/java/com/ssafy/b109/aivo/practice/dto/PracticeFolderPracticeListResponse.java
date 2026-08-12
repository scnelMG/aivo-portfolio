package com.ssafy.b109.aivo.practice.dto;

import java.util.List;

public record PracticeFolderPracticeListResponse(
        Long attemptCount,
        int currentPage,
        int totalPages,
        boolean hasNext,
        List<PracticeFolderPracticeResponse> practices
) {
}
