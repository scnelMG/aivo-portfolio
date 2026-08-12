package com.ssafy.b109.aivo.presentation.dto;

import com.ssafy.b109.aivo.presentation.entity.PresentationReportJob;

import java.time.LocalDateTime;
import java.util.UUID;

public record PresentationReportJobStatusResponse(
        Long presentationId,
        Long practiceId,
        Long audioId,
        UUID requestId,
        String status,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PresentationReportJobStatusResponse of(
            Long presentationId,
            Long practiceId,
            PresentationReportJob job
    ) {
        return new PresentationReportJobStatusResponse(
                presentationId,
                practiceId,
                job.getAudioId(),
                job.getRequestId(),
                job.getStatus().name(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
