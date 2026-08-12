package com.ssafy.b109.aivo.media.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audio_segments")
@Getter
@Setter
public class AudioSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_id", nullable = false, foreignKey = @ForeignKey(name = "fk_audio_segments_audio"))
    private Audio audio;

    @Column(name = "sequence", nullable = false)
    private Integer sequence;

    @Column(name = "start_sec", nullable = false)
    private Double startSec;

    @Column(name = "end_sec", nullable = false)
    private Double endSec;

    @Column(name = "start_time_ms", nullable = false)
    private Long startTimeMs;

    @Column(name = "end_time_ms", nullable = false)
    private Long endTimeMs;

    @Column(name = "text", nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
