package com.ssafy.b109.aivo.nonverbal.repository;

import com.ssafy.b109.aivo.nonverbal.entity.NonverbalAnalysisLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NonverbalAnalysisLogRepository extends JpaRepository<NonverbalAnalysisLog, Long> {

    List<NonverbalAnalysisLog> findAllByPracticeIdOrderByCreatedAtAsc(Long practiceId);


    @Query(
            value = """
                    select *
                    from nonverbal_analysis_logs
                    where practice_id = :practiceId
                      and event_type = 'VIDEO_GAZE_DEVIATION'
                    order by created_at desc, id desc
                    limit 1
                    """,
            nativeQuery = true
    )
    Optional<NonverbalAnalysisLog> findGazeLogByPracticeId(@Param("practiceId") Long practiceId);

    @Query(
            value = """
                    select cast(greatest(0, least(100, 100 - coalesce(avg(metric_value), 0))) as double precision)
                    from nonverbal_analysis_logs
                    where practice_id = :practiceId
                      and event_type = 'VIDEO_POSTURE_TILT'
                      and metric_value is not null
                    """,
            nativeQuery = true
    )
    Double postureStabilityScoreByPracticeId(@Param("practiceId") Long practiceId);

    void deleteByPracticeIdAndEventTypeIn(Long practiceId, Collection<String> eventTypes);

    NonverbalAnalysisLog findByPracticeIdAndEventType(Long practiceId, String videoGazeDeviation);
}
