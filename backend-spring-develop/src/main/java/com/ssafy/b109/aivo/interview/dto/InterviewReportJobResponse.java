package com.ssafy.b109.aivo.interview.dto;

import com.ssafy.b109.aivo.interview.entity.InterviewReportJob;

public record InterviewReportJobResponse(
        Long interviewId,
        Long practiceId,
        String status,
        String errorMessage
) {
    public static InterviewReportJobResponse from(InterviewReportJob job) {
        return new InterviewReportJobResponse(
                job.getInterviewId(),
                job.getPracticeId(),
                job.getStatus().name(),
                job.getErrorMessage()
        );
    }
}
