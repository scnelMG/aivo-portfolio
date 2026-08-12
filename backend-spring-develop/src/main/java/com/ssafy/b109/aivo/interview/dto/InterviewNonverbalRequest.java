package com.ssafy.b109.aivo.interview.dto;

import java.util.List;

public record InterviewNonverbalRequest(
        Integer gazeDeviationCount,
        Integer postureTiltPercent,
        Integer sampleCount,
        List<QuestionGestureEventResponse> gazeEvents,
        List<QuestionGestureBucketResponse> tiltBuckets
) {
}
