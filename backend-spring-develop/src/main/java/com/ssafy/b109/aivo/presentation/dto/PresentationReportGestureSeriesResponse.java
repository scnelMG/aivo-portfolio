package com.ssafy.b109.aivo.presentation.dto;

import java.util.List;

public record PresentationReportGestureSeriesResponse(
        List<PresentationReportGestureBucketResponse> buckets,
        Integer gazeCount,
        List<PresentationReportGazeEventResponse> gazeEvents
) {
}
