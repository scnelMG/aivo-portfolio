package com.ssafy.b109.aivo.interview.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "interview_report_jobs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_interview_report_jobs_practice_id", columnNames = "practice_id"),
                @UniqueConstraint(name = "uq_interview_report_jobs_request_id", columnNames = "request_id")
        }
)
@Getter
@Setter
public class InterviewReportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "interview_id", nullable = false)
    private Long interviewId;

    @Column(name = "practice_id", nullable = false)
    private Long practiceId;

    @Column(name = "audio_id")
    private Long audioId;

    @Column(name = "request_id", nullable = false, columnDefinition = "uuid")
    private UUID requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InterviewReportJobStatus status;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
