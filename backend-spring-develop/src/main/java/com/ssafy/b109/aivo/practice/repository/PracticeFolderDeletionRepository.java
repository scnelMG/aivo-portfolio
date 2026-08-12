package com.ssafy.b109.aivo.practice.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class PracticeFolderDeletionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PracticeFolderDeletionPlan createPlan(Long folderId) {
        MapSqlParameterSource folderParameter =
                new MapSqlParameterSource("folderId", folderId);

        List<Long> practiceIds = queryIds(
                """
                SELECT id
                FROM practices
                WHERE folder_id = :folderId
                """,
                folderParameter
        );

        if (practiceIds.isEmpty()) {
            return new PracticeFolderDeletionPlan(
                    folderId,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    Set.of(),
                    Set.of(),
                    Set.of()
            );
        }

        MapSqlParameterSource practiceParameter =
                new MapSqlParameterSource("practiceIds", practiceIds);

        List<Long> presentationIds = queryIds(
                """
                SELECT presentation_id
                FROM practices
                WHERE id IN (:practiceIds)
                  AND presentation_id IS NOT NULL
                """,
                practiceParameter
        );

        List<Long> interviewIds = queryIds(
                """
                SELECT interview_session_id
                FROM practices
                WHERE id IN (:practiceIds)
                  AND interview_session_id IS NOT NULL
                """,
                practiceParameter
        );

        List<Long> totalFeedbackIds = queryIds(
                """
                SELECT id
                FROM total_feedbacks
                WHERE practice_id IN (:practiceIds)
                """,
                practiceParameter
        );

        List<Long> audioIds = queryIds(
                """
                SELECT id
                FROM audios
                WHERE practice_id IN (:practiceIds)
                """,
                practiceParameter
        );

        List<Long> presentationSlideIds = presentationIds.isEmpty()
                ? List.of()
                : queryIds(
                        """
                        SELECT id
                        FROM presentation_slides
                        WHERE presentation_id IN (:presentationIds)
                        """,
                        new MapSqlParameterSource(
                                "presentationIds",
                                presentationIds
                        )
                );

        List<Long> presentationQuestionIds = presentationIds.isEmpty()
                ? List.of()
                : queryIds(
                        """
                        SELECT id
                        FROM presentation_questions
                        WHERE presentation_id IN (:presentationIds)
                        """,
                        new MapSqlParameterSource(
                                "presentationIds",
                                presentationIds
                        )
                );

        List<Long> interviewQuestionIds = interviewIds.isEmpty()
                ? List.of()
                : queryIds(
                        """
                        SELECT id
                        FROM interview_question
                        WHERE interview_id IN (:interviewIds)
                        """,
                        new MapSqlParameterSource(
                                "interviewIds",
                                interviewIds
                        )
                );

        Set<String> slideImageKeys = presentationIds.isEmpty()
                ? Set.of()
                : queryStrings(
                        """
                        SELECT DISTINCT image
                        FROM presentation_slides
                        WHERE presentation_id IN (:presentationIds)
                        """,
                        new MapSqlParameterSource(
                                "presentationIds",
                                presentationIds
                        )
                );

        Set<String> temporaryPresentationKeys = presentationIds.isEmpty()
                ? Set.of()
                : queryStrings(
                        """
                        SELECT DISTINCT temporary_file_key
                        FROM presentations
                        WHERE id IN (:presentationIds)
                          AND temporary_file_key IS NOT NULL
                        """,
                        new MapSqlParameterSource(
                                "presentationIds",
                                presentationIds
                        )
                );

        Set<String> mediaObjectPaths = queryStrings(
                """
                SELECT path
                FROM audios
                WHERE practice_id IN (:practiceIds)

                UNION

                SELECT path
                FROM videos
                WHERE practice_id IN (:practiceIds)
                """,
                practiceParameter
        );

        return new PracticeFolderDeletionPlan(
                folderId,
                List.copyOf(practiceIds),
                List.copyOf(presentationIds),
                List.copyOf(interviewIds),
                List.copyOf(totalFeedbackIds),
                List.copyOf(audioIds),
                List.copyOf(presentationSlideIds),
                List.copyOf(presentationQuestionIds),
                List.copyOf(interviewQuestionIds),
                Set.copyOf(slideImageKeys),
                Set.copyOf(temporaryPresentationKeys),
                Set.copyOf(mediaObjectPaths)
        );
    }

    public void delete(PracticeFolderDeletionPlan plan) {
        deleteByIds(
                "presentation_question_feedbacks",
                "total_feedback_id",
                plan.totalFeedbackIds()
        );
        deleteByIds(
                "presentation_question_feedbacks",
                "question_id",
                plan.presentationQuestionIds()
        );
        deleteByIds(
                "presentation_slide_feedbacks",
                "total_feedback_id",
                plan.totalFeedbackIds()
        );
        deleteByIds(
                "presentation_slide_feedbacks",
                "slide_id",
                plan.presentationSlideIds()
        );
        deleteByIds(
                "presentation_scores",
                "total_feedback_id",
                plan.totalFeedbackIds()
        );
        deleteByIds(
                "interview_scores",
                "total_feedback_id",
                plan.totalFeedbackIds()
        );

        deleteByIds(
                "presentation_report_jobs",
                "practice_id",
                plan.practiceIds()
        );
        deleteByIds(
                "interview_report_jobs",
                "practice_id",
                plan.practiceIds()
        );

        deleteByIds(
                "audio_segments",
                "audio_id",
                plan.audioIds()
        );
        deleteByIds(
                "audio_stt",
                "audio_id",
                plan.audioIds()
        );

        deleteByPracticeIds("connection_logs", plan.practiceIds());
        deleteByPracticeIds("nonverbal_analysis_logs", plan.practiceIds());
        deleteByPracticeIds("nonverbal_metrics", plan.practiceIds());
        deleteByPracticeIds("speech_analysis_logs", plan.practiceIds());
        deleteByPracticeIds("speech_metrics", plan.practiceIds());
        deleteByPracticeIds("slide_click_logs", plan.practiceIds());

        deleteByIds(
                "interview_answer",
                "question_id",
                plan.interviewQuestionIds()
        );
        deleteByIds(
                "question_feedbacks",
                "question_id",
                plan.interviewQuestionIds()
        );
        deleteByIds(
                "question_feedbacks",
                "interview_id",
                plan.interviewIds()
        );

        deleteByIds(
                "interview_feedbacks",
                "interview_id",
                plan.interviewIds()
        );
        deleteByIds(
                "interview_portfolio_mapping",
                "interview_id",
                plan.interviewIds()
        );
        deleteByIds(
                "interview_resume_mapping",
                "interview_id",
                plan.interviewIds()
        );

        deleteByIds(
                "presentation_questions",
                "id",
                plan.presentationQuestionIds()
        );
        deleteByIds(
                "presentation_slides",
                "id",
                plan.presentationSlideIds()
        );
        deleteByIds(
                "interview_question",
                "id",
                plan.interviewQuestionIds()
        );

        deleteByIds(
                "total_feedbacks",
                "id",
                plan.totalFeedbackIds()
        );
        deleteByIds("audios", "id", plan.audioIds());
        deleteByPracticeIds("videos", plan.practiceIds());

        deleteByIds("practices", "id", plan.practiceIds());
        deleteByIds(
                "presentations",
                "id",
                plan.presentationIds()
        );
        deleteByIds("interviews", "id", plan.interviewIds());

        jdbcTemplate.update(
                "DELETE FROM practice_folder WHERE id = :folderId",
                Map.of("folderId", plan.folderId())
        );
    }

    private List<Long> queryIds(
            String sql,
            MapSqlParameterSource parameters
    ) {
        return jdbcTemplate.queryForList(
                sql,
                parameters,
                Long.class
        );
    }

    private Set<String> queryStrings(
            String sql,
            MapSqlParameterSource parameters
    ) {
        List<String> values = jdbcTemplate.queryForList(
                sql,
                parameters,
                String.class
        );

        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(
                        java.util.stream.Collectors.toCollection(
                                LinkedHashSet::new
                        )
                );
    }

    private void deleteByPracticeIds(
            String table,
            Collection<Long> practiceIds
    ) {
        deleteByIds(table, "practice_id", practiceIds);
    }

    private void deleteByIds(
            String table,
            String column,
            Collection<Long> ids
    ) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        jdbcTemplate.update(
                "DELETE FROM " + table +
                        " WHERE " + column + " IN (:ids)",
                Map.of("ids", ids)
        );
    }
}
