package com.ssafy.b109.aivo.interview.event;

import com.ssafy.b109.aivo.interview.service.InterviewAsyncReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class InterviewAudioAnalysisRequestedEventListener {

    private final InterviewAsyncReportService interviewAsyncReportService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(InterviewAudioAnalysisRequestedEvent event) {
        interviewAsyncReportService.publishAudioAnalysisRequest(event.jobId());
    }
}
