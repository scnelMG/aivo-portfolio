package com.ssafy.b109.aivo.presentation.service;

import com.ssafy.b109.aivo.presentation.entity.PresentationReportJob;
import com.ssafy.b109.aivo.presentation.entity.PresentationReportJobStatus;
import com.ssafy.b109.aivo.presentation.repository.PresentationReportJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PresentationReportJobStatusService {

    private final PresentationReportJobRepository presentationReportJobRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long practiceId, Exception exception) {
        markFailed(practiceId, exception.getMessage());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long practiceId, String errorMessage) {
        PresentationReportJob job = presentationReportJobRepository.findByPracticeId(practiceId)
                .orElseThrow(() -> new RuntimeException("presentationReportJob을 찾을 수 없습니다."));

        job.setStatus(PresentationReportJobStatus.FAILED);
        job.setErrorMessage(errorMessage);
        job.setUpdatedAt(LocalDateTime.now());

        presentationReportJobRepository.saveAndFlush(job);
    }
}
