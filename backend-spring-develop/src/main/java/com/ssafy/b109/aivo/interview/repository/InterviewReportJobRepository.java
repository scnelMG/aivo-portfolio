package com.ssafy.b109.aivo.interview.repository;

import com.ssafy.b109.aivo.interview.entity.InterviewReportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewReportJobRepository extends JpaRepository<InterviewReportJob, Long> {

    Optional<InterviewReportJob> findByPracticeId(Long practiceId);

    Optional<InterviewReportJob> findFirstByInterviewIdOrderByCreatedAtDescIdDesc(Long interviewId);

    Optional<InterviewReportJob> findByRequestId(UUID requestId);
}
