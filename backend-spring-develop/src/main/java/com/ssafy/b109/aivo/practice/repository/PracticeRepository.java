package com.ssafy.b109.aivo.practice.repository;

import com.ssafy.b109.aivo.practice.entity.Practice;
import com.ssafy.b109.aivo.practice.repository.projection.PracticeScoreTrendProjection;
import com.ssafy.b109.aivo.presentation.entity.PresentationProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PracticeRepository extends JpaRepository<Practice, Long> {
    Optional<Practice> findByPresentationId(Long presentationId);
    @Query("""
        select count(p) > 0
        from Practice p
        where p.presentation.id = :presentationId
        and p.folder.user.id = :userId
    """)
    boolean existsOwnedPresentation(
            @Param("presentationId")
            Long presentationId,

            @Param("userId")
            Long userId
    );

    Optional<Practice> findByInterviewSession_IdAndFolder_User_Id(Long interviewId, Long userId);

    Optional<Practice> findByIdAndFolder_User_Id(Long presentationId, Long userId);

    Optional<Practice> findByInterviewSession_Id(Long interviewId);

    long countByFolder_Id(Long folderId);
    Optional<Practice> findByPresentation_IdAndFolder_IdAndFolder_User_Id(Long presentationId, Long folderId, Long userId);
    Optional<Practice> findByPresentation_IdAndFolder_User_Id(Long presentationId, Long userId);

    @Query("""                                                                                                                                                                                                                                                                                                        
      select practice                                                                                                                                                                                                                                                                                               
      from Practice practice                                                                                                                                                                                                                                                                                        
      join fetch practice.presentation presentation                                                                                                                                                                                                                                                                 
      where practice.folder.id = :folderId                                                                                                                                                                                                                                                                          
        and practice.folder.user.id = :userId                                                                                                                                                                                                                                                                       
        and presentation.processingStatus = :status                                                                                                                                                                                                                                                                 
      order by practice.createdAt desc                                                                                                                                                                                                                                                                              
  """)
    List<Practice> findReusablePresentationPractices(
            @Param("folderId") Long folderId,
            @Param("userId") Long userId,
            @Param("status")
            PresentationProcessingStatus status
    );

    @Query(
            value = """
                  SELECT COUNT(*)
                  FROM practices practice
                  JOIN practice_folder folder
                      ON folder.id = practice.folder_id
                  WHERE folder.user_id = :userId
                  """,
            nativeQuery = true
    )
    Long countAllByUserId(@Param("userId") Long userId);

    @Query(
            value = """
                  SELECT *
                  FROM (
                      SELECT
                          practice.id AS "practiceId",
                          practice.created_at AS "practicedAt",
                          COALESCE(
                              presentation_score.overall_score,
                              interview_score.overall_score
                          ) AS "overallScore",
                          COALESCE(
                              presentation_score.voice_score,
                              interview_score.voice_score
                          ) AS "voiceScore",
                          COALESCE(
                              presentation_score.video_score,
                              interview_score.video_score
                          ) AS "videoScore",
                          COALESCE(
                              presentation_score.content_score,
                              interview_score.content_score
                          ) AS "contentScore"
                      FROM practices practice
                      JOIN practice_folder folder
                          ON folder.id = practice.folder_id
                      JOIN total_feedbacks feedback
                          ON feedback.practice_id = practice.id
                      LEFT JOIN presentation_scores presentation_score
                          ON presentation_score.total_feedback_id = feedback.id
                      LEFT JOIN interview_scores interview_score
                          ON interview_score.total_feedback_id = feedback.id
                      WHERE folder.user_id = :userId
                        AND (
                              (
                                  practice.presentation_id IS NOT NULL
                                  AND presentation_score.id IS NOT NULL
                              )
                              OR (
                                  practice.interview_session_id IS NOT NULL
                                  AND interview_score.id IS NOT NULL
                              )
                        )
                      ORDER BY practice.created_at DESC, practice.id DESC
                      LIMIT :window
                  ) recent_practices
                  ORDER BY "practicedAt" ASC, "practiceId" ASC
                  """,
            nativeQuery = true
    )
    List<PracticeScoreTrendProjection> findRecentScoreTrendsByUserId(
            @Param("userId") Long userId,
            @Param("window") int window
    );
}
