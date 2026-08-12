package com.ssafy.b109.aivo.presentation.dto;

import java.net.URI;

public record PresentationReportSlideResponse(
        Long slideId,
        Integer slideNumber,
        URI imageUrl,
        String coreContent,
        Float startTimeSec,
        Float endTimeSec,
        PresentationReportSlideFeedbackResponse feedback
) {
}