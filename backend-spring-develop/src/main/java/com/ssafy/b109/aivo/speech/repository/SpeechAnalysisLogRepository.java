package com.ssafy.b109.aivo.speech.repository;

import com.ssafy.b109.aivo.speech.entity.SpeechAnalysisLog;
import com.ssafy.b109.aivo.speech.repository.projection.SpeechSpeedSectionAverageProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpeechAnalysisLogRepository extends JpaRepository<SpeechAnalysisLog, Long> {

    List<SpeechAnalysisLog> findAllByPracticeIdOrderByCreatedAtAsc(Long practiceId);

    List<SpeechAnalysisLog> findAllByPracticeIdOrderByStartTimeMsAscIdAsc(Long practiceId);

    @Query(
            value = """
                    select cast(coalesce(sum((metadata ->> 'fillerCount')::double precision), 0) as double precision)
                    from speech_analysis_logs
                    where practice_id = :practiceId
                      and event_type = 'AUDIO_CHUNK_ANALYSIS'
                    """,
            nativeQuery = true
    )
    Double sumFillerCountByPracticeId(@Param("practiceId") Long practiceId);

    @Query(
            value = """
                    select cast(coalesce(avg(nullif(coalesce((metadata ->> 'averageWpm')::double precision, metric_value::double precision), 0)), 0) as double precision)
                    from speech_analysis_logs
                    where practice_id = :practiceId
                      and event_type = 'AUDIO_CHUNK_ANALYSIS'
                    """,
            nativeQuery = true
    )
    Double averageWpmByPracticeId(@Param("practiceId") Long practiceId);

    @Query(
            value = """
                    select cast(coalesce(
                        ((max(metric_value) - min(metric_value)) / nullif(avg(metric_value), 0)) * 100,
                        0
                    ) as double precision)
                    from speech_analysis_logs
                    where practice_id = :practiceId
                      and event_type = 'AUDIO_CHUNK_ANALYSIS'
                      and metric_value is not null
                      and metric_value > 0
                    """,
            nativeQuery = true
    )
    Double speedChangePercentByPracticeId(@Param("practiceId") Long practiceId);

    @Query(
            value = """
                    SELECT AVG(log.metric_value)
                    FROM speech_analysis_logs log
                    JOIN practices practice
                        ON practice.id = log.practice_id
                    JOIN practice_folder folder
                        ON folder.id = practice.folder_id
                    WHERE folder.user_id = :userId
                      AND log.metric_value IS NOT NULL
                    """,
            nativeQuery = true
    )
    Double findAverageMetricValueByUserId(@Param("userId") Long userId);

    @Query(
            value = """
                    WITH latest_practice_duration AS (
                        SELECT DISTINCT ON (nonverbal_log.practice_id)
                            nonverbal_log.practice_id,
                            nonverbal_log.end_time_ms AS total_time_ms
                        FROM nonverbal_analysis_logs nonverbal_log
                        JOIN practices practice
                            ON practice.id = nonverbal_log.practice_id
                        JOIN practice_folder folder
                            ON folder.id = practice.folder_id
                        WHERE folder.user_id = :userId
                          AND nonverbal_log.event_type = 'VIDEO_GAZE_DEVIATION'
                          AND nonverbal_log.end_time_ms IS NOT NULL
                        ORDER BY nonverbal_log.practice_id,
                                 nonverbal_log.created_at DESC,
                                 nonverbal_log.id DESC
                    ),
                    practice_section_speed AS (
                        SELECT
                            duration.practice_id,
                            AVG(
                                CASE
                                    WHEN speech_log.end_time_ms <= duration.total_time_ms * 0.3
                                    THEN speech_log.metric_value
                                END
                            ) AS early_speech_speed,
                            AVG(
                                CASE
                                    WHEN speech_log.start_time_ms >= duration.total_time_ms * 0.7
                                    THEN speech_log.metric_value
                                END
                            ) AS late_speech_speed
                        FROM latest_practice_duration duration
                        JOIN speech_analysis_logs speech_log
                            ON speech_log.practice_id = duration.practice_id
                        WHERE speech_log.metric_value IS NOT NULL
                        GROUP BY duration.practice_id
                    )
                    SELECT
                        AVG(early_speech_speed) AS "earlySpeechSpeed",
                        AVG(late_speech_speed) AS "lateSpeechSpeed"
                    FROM practice_section_speed
                    """,
            nativeQuery = true
    )
    SpeechSpeedSectionAverageProjection findSpeechSpeedSectionAveragesByUserId(
            @Param("userId") Long userId
    );

    @Query(
            value = """
                    WITH latest_practice_duration AS (
                        SELECT DISTINCT ON (nonverbal_log.practice_id)
                            nonverbal_log.practice_id,
                            nonverbal_log.end_time_ms AS total_time_ms
                        FROM nonverbal_analysis_logs nonverbal_log
                        JOIN practices practice
                            ON practice.id = nonverbal_log.practice_id
                        JOIN practice_folder folder
                            ON folder.id = practice.folder_id
                        WHERE folder.user_id = :userId
                          AND nonverbal_log.event_type = 'VIDEO_GAZE_DEVIATION'
                          AND nonverbal_log.end_time_ms IS NOT NULL
                          AND nonverbal_log.end_time_ms > 0
                        ORDER BY nonverbal_log.practice_id,
                                 nonverbal_log.created_at DESC,
                                 nonverbal_log.id DESC
                    ),
                    total_silence AS (
                        SELECT
                            COALESCE(
                                SUM(
                                    COALESCE(
                                        NULLIF(speech_log.metadata ->> 'silenceDurationMs', '')::double precision,
                                        0
                                    )
                                ),
                                0
                            ) AS silence_duration_ms
                        FROM speech_analysis_logs speech_log
                        JOIN latest_practice_duration duration
                            ON duration.practice_id = speech_log.practice_id
                    )
                    SELECT
                        total_silence.silence_duration_ms
                            / NULLIF(SUM(duration.total_time_ms), 0)
                    FROM latest_practice_duration duration
                    CROSS JOIN total_silence
                    GROUP BY total_silence.silence_duration_ms
                    """,
            nativeQuery = true
    )
    Double findSilenceDurationRatioByUserId(@Param("userId") Long userId);

    List<SpeechAnalysisLog> findByPracticeId(Long practiceId);
}
