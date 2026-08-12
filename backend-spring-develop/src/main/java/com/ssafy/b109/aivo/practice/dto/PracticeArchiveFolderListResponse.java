package com.ssafy.b109.aivo.practice.dto;

import java.util.List;

public record PracticeArchiveFolderListResponse(
        long totalElements,
        int currentPage,
        int totalPage,
        boolean hasNext,
        List<PracticeArchiveFolderResponse> folders
)  {
}
