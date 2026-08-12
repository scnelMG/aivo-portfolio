package com.ssafy.b109.aivo.presentation.repository;

import com.ssafy.b109.aivo.practice.entity.Practice;
import com.ssafy.b109.aivo.presentation.entity.PresentationReportJob;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PresentationReportJobRepository extends JpaRepository<PresentationReportJob, Long> {
    Optional<PresentationReportJob> findByPractice(Practice practice);

    Optional<PresentationReportJob> findByPracticeId(Long practiceId);
}
