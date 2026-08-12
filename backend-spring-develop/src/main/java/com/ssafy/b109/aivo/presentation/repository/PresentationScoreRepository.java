package com.ssafy.b109.aivo.presentation.repository;

import com.ssafy.b109.aivo.presentation.entity.PresentationScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PresentationScoreRepository extends JpaRepository<PresentationScore, Long> {

    @Query("""
        select score
        from PresentationScore score
        join TotalFeedback feedback
        on feedback.id = score.totalFeedbackId
        where feedback.practiceId = :practiceId
    """)
    Optional<PresentationScore> findByPracticeId(@Param("practiceId") Long practiceId);

    @Query("""
        select avg(score.overallScore)
        from PresentationScore score
        join TotalFeedback feedback
        on feedback.id = score.totalFeedbackId
        join Practice practice
        on practice.id = feedback.practiceId
        where practice.folder.id = :folderId
        and practice.presentation is not null
        and score.overallScore is not null
    """)
    Double findFolderAverageScore(@Param("folderId") Long folderId);
    Optional<PresentationScore> findByTotalFeedbackId(Long totalFeedbackId);
}
