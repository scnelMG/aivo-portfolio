package com.ssafy.b109.aivo.practice.repository;

import com.ssafy.b109.aivo.practice.entity.PracticeFolder;
import com.ssafy.b109.aivo.practice.entity.PracticeType;
import com.ssafy.b109.aivo.practice.repository.projection.PracticeArchiveStatisticsProjection;
import com.ssafy.b109.aivo.practice.repository.projection.PracticeFolderDetailStatisticsProjection;
import com.ssafy.b109.aivo.practice.repository.projection.PracticeFolderPracticeProjection;
import com.ssafy.b109.aivo.practice.repository.projection.PracticeScoreTrendProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PracticeFolderRepository extends JpaRepository<PracticeFolder, Long> {

    Optional<PracticeFolder> findFirstByUserIdAndNameOrderByIdAsc(Long userId, String name);

    Optional<PracticeFolder> findByIdAndUserId(Long folderId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT folder
            FROM PracticeFolder folder
            WHERE folder.id = :folderId
              AND folder.user.id = :userId
            """)
    Optional<PracticeFolder> findByIdAndUserIdForUpdate(
            @Param("folderId") Long folderId,
            @Param("userId") Long userId
    );

    List<PracticeFolder> findAllByUserIdOrderByIdDesc(Long userId);

    List<PracticeFolder> findAllByUserIdAndNameContainingIgnoreCaseOrderByIdDesc(Long userId, String keyword);

    List<PracticeFolder> findAllByUserIdAndTypeOrderByIdDesc(
            Long userId,
            PracticeType type
    );

    List<PracticeFolder> findAllByUserIdAndTypeAndNameContainingIgnoreCaseOrderByIdDesc(
            Long userId,
            PracticeType type,
            String keyword
    );

    @Query(
            value = """                                                                                                                                                   
              WITH completed_practices AS (                                                                                                                             
                                                                                                                                                                        
                  SELECT                                                                                                                                                
                      folder.id                                                                                                                                         
                          AS folder_id,                                                                                                                                 
                                                                                                                                                                        
                      'presentation'                                                                                                                                    
                          AS practice_type,                                                                                                                             
                                                                                                                                                                        
                      folder.name                                                                                                                                       
                          AS folder_name,                                                                                                                               
                                                                                                                                                                        
                      folder.description                                                                                                                                
                          AS folder_description,                                                                                                                        
                                                                                                                                                                        
                      practice.id                                                                                                                                       
                          AS practice_id,                                                                                                                               
                                                                                                                                                                        
                      practice.created_at                                                                                                                               
                          AS practice_created_at,                                                                                                                       
                                                                                                                                                                        
                      score.overall_score                                                                                                                               
                          AS overall_score                                                                                                                              
                                                                                                                                                                        
                  FROM practices practice                                                                                                                               
                                                                                                                                                                        
                  JOIN practice_folder folder                                                                                                                           
                      ON folder.id = practice.folder_id                                                                                                                 
                                                                                                                                                                        
                  JOIN total_feedbacks feedback                                                                                                                         
                      ON feedback.practice_id = practice.id                                                                                                             
                                                                                                                                                                        
                  JOIN presentation_scores score                                                                                                                        
                      ON score.total_feedback_id = feedback.id                                                                                                          
                                                                                                                                                                        
                  WHERE folder.user_id = :userId                                                                                                                        
                                                                                                                                                                        
                    AND folder.type = 'PRESENTATION'
                                                                                                                                                                        
                    AND score.overall_score IS NOT NULL                                                                                                                 
                                                                                                                                                                        
                    AND (                                                                                                                                               
                        :type = ''                                                                                                                                      
                        OR folder.type = :type
                    )                                                                                                                                                   
                                                                                                                                                                        
                    AND (                                                                                                                                               
                        :keyword = ''                                                                                                                                   
                        OR LOWER(folder.name) LIKE LOWER(                                                                                                               
                            CONCAT(                                                                                                                                     
                                '%',                                                                                                                                    
                                :keyword,                                                                                                                               
                                '%'                                                                                                                                     
                            )                                                                                                                                           
                        )
                    )                                                                                                                                                   
                                                                                                                                                                        
                  UNION ALL                                                                                                                                             
                                                                                                                                                                        
                  SELECT                                                                                                                                                
                      folder.id                                                                                                                                         
                          AS folder_id,                                                                                                                                 
                                                                                                                                                                        
                      'interview'                                                                                                                                       
                          AS practice_type,                                                                                                                             
                                                                                                                                                                        
                      folder.name                                                                                                                                       
                          AS folder_name,                                                                                                                               
 
                      folder.description                                                                                                                                
                          AS folder_description,                                                                                                                        
                                                                                                                                                                        
                      practice.id                                                                                                                                       
                          AS practice_id,                                                                                                                               
                                                                                                                                                                        
                      practice.created_at                                                                                                                               
                          AS practice_created_at,                                                                                                                       
                                                                                                                                                                        
                      score.overall_score                                                                                                                               
                          AS overall_score                                                                                                                              
                                                                                                                                                                        
                  FROM practices practice                                                                                                                               
                                                                                                                                                                        
                  JOIN practice_folder folder                                                                                                                           
                      ON folder.id = practice.folder_id                                                                                                                 
                                                                                                                                                                        
                  JOIN total_feedbacks feedback                                                                                                                         
                      ON feedback.practice_id = practice.id                                                                                                             
                                                                                                                                                                        
                  JOIN interview_scores score                                                                                                                           
                      ON score.total_feedback_id = feedback.id                                                                                                          
                                                                                                                                                                        
                  WHERE folder.user_id = :userId                                                                                                                        
                                                                                                                                                                        
                    AND folder.type = 'INTERVIEW'
                                                                                                                                                                        
                    AND score.overall_score IS NOT NULL                                                                                                                 
                                                                                                                                                                        
                    AND (                                                                                                                                               
                        :type = ''                                                                                                                                      
                        OR folder.type = :type
                    )                                                                                                                                                   
                                                                                                                                                                        
                    AND (                                                                                                                                               
                        :keyword = ''                                                                                                                                   
                        OR LOWER(folder.name) LIKE LOWER(                                                                                                               
                            CONCAT(                                                                                                                                     
                                '%',                                                                                                                                    
                                :keyword,                                                                                                                               
                                '%'                                                                                                                                     
                            )                                                                                                                                           
                        )                                                                                                                                               
                    )                                                                                                                                                   
              ),                                                                                                                                                        
                                                                                                                                                                        
              ranked_practices AS (                                                                                                                                     
                  SELECT                                                                                                                                                
                      completed_practices.*,                                                                                                                            
                                                                                                                                                                        
                      ROW_NUMBER() OVER (                                                                                                                               
                          PARTITION BY                                                                                                                                  
                              folder_id,                                                                                                                                
                              practice_type                                                                                                                             
                          ORDER BY                                                                                                                                      
                              practice_created_at DESC,                                                                                                                 
                              practice_id DESC                                                                                                                          
                      ) AS row_number                                                                                                                                   
                                                                                                                                                                        
                  FROM completed_practices                                                                                                                              
              )                                                                                                                                                         
                                                                                                                                                                        
              SELECT                                                                                                                                                    
                  folder_id                                                                                                                                             
                      AS "folderId",                                                                                                                                    
                                                                                                                                                                        
                  practice_type                                                                                                                                         
                      AS "type",                                                                                                                                        
                                                                                                                                                                        
                  MAX(practice_created_at)                                                                                                                              
                      FILTER (                                                                                                                                          
                          WHERE row_number = 1                                                                                                                          
                      )                                                                                                                                                 
                      AS "recentPracticeDate",                                                                                                                          
                                                                                                                                                                        
                  folder_name                                                                                                                                           
                      AS "name",                                                                                                                                        
                                                                                                                                                                        
                  CAST(                                                                                                                                                 
                      ROUND(                                                                                                                                            
                          AVG(overall_score)                                                                                                                            
                      )                                                                                                                                                 
                      AS SMALLINT                                                                                                                                       
                  )                                                                                                                                                     
                      AS "averageScore",                                                                                                                                
                                                                                                                                                                        
                  folder_description                                                                                                                                    
                      AS "description",                                                                                                                                 
                                                                                                                                                                        
                  CAST(                                                                                                                                                 
                      COUNT(*)                                                                                                                                          
                      AS BIGINT                                                                                                                                         
                  )                                                                                                                                                     
                      AS "attemptCount",                                                                                                                                
                                                                                                                                                                        
                  CAST(                                                                                                                                                 
                      MAX(overall_score)                                                                                                                                
                      AS SMALLINT                                                                                                                                       
                  )                                                                                                                                                     
                      AS "maxScore",                                                                                                                                    
                                                                                                                                                                        
                  CAST(                                                                                                                                                 
                      MAX(overall_score)                                                                                                                                
                          FILTER (                                                                                                                                      
                              WHERE row_number = 1                                                                                                                      
                          )                                                                                                                                             
                      AS SMALLINT                                                                                                                                       
                  )                                                                                                                                                     
                      AS "recentScore"                                                                                                                                  
                                                                                                                                                                        
              FROM ranked_practices                                                                                                                                     
                                                                                                                                                                        
              GROUP BY                                                                                                                                                  
                  folder_id,                                                                                                                                            
                  practice_type,                                                                                                                                        
                  folder_name,                                                                                                                                          
                  folder_description                                                                                                                                    
                                                                                                                                                                        
              ORDER BY                                                                                                                                                  
                  "recentPracticeDate" DESC,                                                                                                                            
                  folder_id DESC                                                                                                                                        
              """,

            countQuery = """                                                                                                                                              
              SELECT COUNT(*)                                                                                                                                           
                                                                                                                                                                        
              FROM (                                                                                                                                                    
                  SELECT DISTINCT                                                                                                                                       
                      archive.folder_id,                                                                                                                                
                      archive.practice_type                                                                                                                             
                                                                                                                                                                        
                  FROM (                                                                                                                                                
                      SELECT                                                                                                                                            
                          folder.id                                                                                                                                     
                              AS folder_id,                                                                                                                             
                                                                                                                                                                        
                          'presentation'                                                                                                                                
                              AS practice_type                                                                                                                          
                                                                                                                                                                        
                      FROM practices practice                                                                                                                           
                                                                                                                                                                        
                      JOIN practice_folder folder                                                                                                                       
                          ON folder.id = practice.folder_id                                                                                                             
                                                                                                                                                                        
                      JOIN total_feedbacks feedback                                                                                                                     
                          ON feedback.practice_id = practice.id                                                                                                         
                                                                                                                                                                        
                      JOIN presentation_scores score                                                                                                                    
                          ON score.total_feedback_id = feedback.id                                                                                                      
                                                                                                                                                                        
                      WHERE folder.user_id = :userId                                                                                                                    
                                                                                                                                                                        
                        AND folder.type = 'PRESENTATION'
                                                                                                                                                                        
                        AND score.overall_score IS NOT NULL                                                                                                             
                                                                                                                                                                        
                        AND (                                                                                                                                           
                            :type = ''                                                                                                                                  
                            OR folder.type = :type
                        )                                                                                                                                               
                                                                                                                                                                        
                        AND (                                                                                                                                           
                            :keyword = ''                                                                                                                               
                            OR LOWER(folder.name) LIKE LOWER(                                                                                                           
                                CONCAT(                                                                                                                                 
                                    '%',                                                                                                                                
                                    :keyword,                                                                                                                           
                                    '%'                                                                                                                                 
                                )                                                                                                                                       
                            )                                                                                                                                           
                        )                                                                                                                                               
                                                                                                                                                                        
                      UNION ALL                                                                                                                                         
                                                                                                                                                                        
                      SELECT                                                                                                                                            
                          folder.id                                                                                                                                     
                              AS folder_id,                                                                                                                             
                                                                                                                                                                        
                          'interview'                                                                                                                                   
                              AS practice_type                                                                                                                          
                                                                                                                                                                        
                      FROM practices practice                                                                                                                           
                                                                                                                                                                        
                      JOIN practice_folder folder                                                                                                                       
                          ON folder.id = practice.folder_id                                                                                                             
                                                                                                                                                                        
                      JOIN total_feedbacks feedback                                                                                                                     
                          ON feedback.practice_id = practice.id                                                                                                         
                                                                                                                                                                        
                      JOIN interview_scores score                                                                                                                       
                          ON score.total_feedback_id = feedback.id                                                                                                      
                                                                                                                                                                        
                      WHERE folder.user_id = :userId                                                                                                                    
                                                                                                                                                                        
                        AND folder.type = 'INTERVIEW'
                                                                                                                                                                        
                        AND score.overall_score IS NOT NULL                                                                                                             
                                                                                                                                                                        
                        AND (                                                                                                                                           
                            :type = ''                                                                                                                                  
                            OR folder.type = :type
                        )                                                                                                                                               
                                                                                                                                                                        
                        AND (                                                                                                                                           
                            :keyword = ''                                                                                                                               
                            OR LOWER(folder.name) LIKE LOWER(                                                                                                           
                                CONCAT(                                                                                                                                 
                                    '%',                                                                                                                                
                                    :keyword,                                                                                                                           
                                    '%'                                                                                                                                 
                                )                                                                                                                                       
                            )                                                                                                                                           
                        )                                                                                                                                               
                  ) archive                                                                                                                                             
              ) archive_folders                                                                                                                                         
              """,

            nativeQuery = true
    )
    Page<PracticeArchiveStatisticsProjection>
    findArchiveStatistics(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query(
            value = """                                                                                                                                                   
                  SELECT                                                                                                                                                
                      CAST(                                                                                                                                             
                          COUNT(*)                                                                                                                                      
                          AS BIGINT                                                                                                                                     
                      ) AS "attemptCount",                                                                                                                              
                                                                                                                                                                        
                      CAST(                                                                                                                                             
                          COALESCE(                                                                                                                                     
                              SUM(practice.duration_sec),                                                                                                               
                              0                                                                                                                                         
                          )                                                                                                                                             
                          AS BIGINT                                                                                                                                     
                      ) AS "totalDuration",                                                                                                                             
                                                                                                                                                                        
                      CAST(                                                                                                                                             
                          COALESCE(                                                                                                                                     
                              MAX(                                                                                                                                      
                                  COALESCE(                                                                                                                             
                                      presentation_score.overall_score,                                                                                                 
                                      interview_score.overall_score                                                                                                     
                                  )                                                                                                                                     
                              ),                                                                                                                                        
                              0                                                                                                                                         
                          )                                                                                                                                             
                          AS BIGINT                                                                                                                                     
                      ) AS "maxScore"                                                                                                                                   
                                                                                                                                                                        
                  FROM practices practice                                                                                                                               
                                                                                                                                                                        
                  JOIN total_feedbacks feedback                                                                                                                         
                      ON feedback.practice_id = practice.id                                                                                                             
                                                                                                                                                                        
                  LEFT JOIN presentation_scores presentation_score                                                                                                      
                      ON presentation_score.total_feedback_id = feedback.id                                                                                             
                                                                                                                                                                        
                  LEFT JOIN interview_scores interview_score                                                                                                            
                      ON interview_score.total_feedback_id = feedback.id                                                                                                
                                                                                                                                                                        
                  WHERE practice.folder_id = :folderId                                                                                                                  
                    AND COALESCE(                                                                                                                                       
                          presentation_score.overall_score,                                                                                                             
                          interview_score.overall_score                                                                                                                 
                        ) IS NOT NULL                                                                                                                                   
                  """,
            nativeQuery = true
    )
    PracticeFolderDetailStatisticsProjection findFolderDetailStatistics(
            @Param("folderId") Long folderId
    );

    @Query("""
      select                                                                                                                                                       \s
          practice.id as practiceId,                                                                                                                               \s
          practice.createdAt as practicedAt,                                                                                                                       \s
          score.overallScore as overallScore,                                                                                                                      \s
          score.voiceScore as voiceScore,                                                                                                                          \s
          score.videoScore as videoScore,                                                                                                                          \s
          score.contentScore as contentScore                                                                                                                       \s
      from PresentationScore score,                                                                                                                                \s
           TotalFeedback feedback,                                                                                                                                 \s
           Practice practice                                                                                                                                       \s
      where score.totalFeedbackId = feedback.id                                                                                                                    \s
        and feedback.practiceId = practice.id                                                                                                                      \s
        and practice.folder.id = :folderId                                                                                                                         \s
        and practice.presentation is not null                                                                                                                      \s
        and score.overallScore is not null                                                                                                                         \s
      order by practice.createdAt desc, practice.id desc
    """)
    List<PracticeScoreTrendProjection> findRecentPresentationScoreTrend(
            @Param("folderId") Long folderId,
            Pageable pageable
    );

    @Query("""                                                                                                                                                        
          select                                                                                                                                                        
              practice.id as practiceId,                                                                                                                                
              practice.createdAt as practicedAt,                                                                                                                        
              score.overallScore as overallScore,                                                                                                                       
              score.voiceScore as voiceScore,                                                                                                                           
              score.videoScore as videoScore,                                                                                                                           
              score.contentScore as contentScore                                                                                                                        
          from InterviewScore score,                                                                                                                                    
               TotalFeedback feedback,                                                                                                                                  
               Practice practice                                                                                                                                        
          where score.totalFeedbackId = feedback.id                                                                                                                     
            and feedback.practiceId = practice.id                                                                                                                       
            and practice.folder.id = :folderId                                                                                                                          
            and practice.interviewSession is not null                                                                                                                   
            and score.overallScore is not null                                                                                                                          
          order by practice.createdAt desc, practice.id desc                                                                                                            
      """)
    List<PracticeScoreTrendProjection>
    findRecentInterviewScoreTrend(
            @Param("folderId") Long folderId,
            Pageable pageable
    );

    @Query(
            value = """                                                                                                                                                   
              select                                                                                                                                                    
                  practice.id as practiceId,
                  practice.presentation.id as targetId,                                                                                                                            
                  practice.title as title,                                                                                                                              
                  practice.durationSec as durationSec,                                                                                                                  
                  score.overallScore as overallScore,                                                                                                                   
                  practice.createdAt as createdAt                                                                                                                       
              from PresentationScore score                                                                                                                              
              join TotalFeedback feedback                                                                                                                               
                  on feedback.id = score.totalFeedbackId
              join Practice practice                                                                                                                                    
                  on practice.id = feedback.practiceId                                                                                                                  
              where practice.folder.id = :folderId                                                                                                                      
                and practice.presentation is not null                                                                                                                   
                and score.overallScore is not null                                                                                                                      
              order by                                                                                                                                                  
                  case                                                                                                                                                  
                      when :sortType = 'scoreAsc'                                                                                                                       
                      then score.overallScore                                                                                                                           
                  end asc,                                                                                                                                              
                  case                                                                                                                                                  
                      when :sortType = 'scoreDesc'                                                                                                                      
                      then score.overallScore                                                                                                                           
                  end desc,                                                                                                                                             
                  practice.createdAt desc,                                                                                                                              
                  practice.id desc                                                                                                                                      
          """,
            countQuery = """                                                                                                                                              
              select count(practice)                                                                                                                                    
              from PresentationScore score                                                                                                                              
              join TotalFeedback feedback                                                                                                                               
                  on feedback.id = score.totalFeedbackId                                                                                                                
              join Practice practice                                                                                                                                    
                  on practice.id = feedback.practiceId                                                                                                                  
              where practice.folder.id = :folderId                                                                                                                      
                and practice.presentation is not null                                                                                                                   
                and score.overallScore is not null                                                                                                                      
          """
    )
    Page<PracticeFolderPracticeProjection> findPresentationPractices(
            @Param("folderId") Long folderId,
            @Param("sortType") String sortType,
            Pageable pageable
    );

    @Query(
            value = """                                                                                                                                                   
              select                                                                                                                                                    
                  practice.id as practiceId,
                  practice.interviewSession.id as targetId,                                                                                                                            
                  practice.title as title,                                                                                                                              
                  practice.durationSec as durationSec,                                                                                                                  
                  score.overallScore as overallScore,                                                                                                                   
                  practice.createdAt as createdAt                                                                                                                       
              from InterviewScore score                                                                                                                                 
              join TotalFeedback feedback                                                                                                                               
                  on feedback.id = score.totalFeedbackId                                                                                                                
              join Practice practice                                                                                                                                    
                  on practice.id = feedback.practiceId                                                                                                                  
              where practice.folder.id = :folderId                                                                                                                      
                and practice.interviewSession is not null                                                                                                               
                and score.overallScore is not null                                                                                                                      
              order by                                                                                                                                                  
                  case                                                                                                                                                  
                      when :sortType = 'scoreAsc'                                                                                                                       
                      then score.overallScore                                                                                                                           
                  end asc,                                                                                                                                              
                  case                                                                                                                                                  
                      when :sortType = 'scoreDesc'                                                                                                                      
                      then score.overallScore                                                                                                                           
                  end desc,                                                                                                                                             
                  practice.createdAt desc,                                                                                                                              
                  practice.id desc                                                                                                                                      
          """,
            countQuery = """                                                                                                                                              
              select count(practice)                                                                                                                                    
              from InterviewScore score                                                                                                                                 
              join TotalFeedback feedback                                                                                                                               
                  on feedback.id = score.totalFeedbackId                                                                                                                
              join Practice practice                                                                                                                                    
                  on practice.id = feedback.practiceId                                                                                                                  
              where practice.folder.id = :folderId                                                                                                                      
                and practice.interviewSession is not null                                                                                                               
                and score.overallScore is not null                                                                                                                      
          """
    )
    Page<PracticeFolderPracticeProjection> findInterviewPractices(
            @Param("folderId") Long folderId,
            @Param("sortType") String sortType,
            Pageable pageable
    );
}
