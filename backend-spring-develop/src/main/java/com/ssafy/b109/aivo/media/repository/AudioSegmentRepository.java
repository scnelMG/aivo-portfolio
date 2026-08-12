package com.ssafy.b109.aivo.media.repository;

import com.ssafy.b109.aivo.media.entity.AudioSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AudioSegmentRepository extends JpaRepository<AudioSegment, Long> {

    void deleteAllByAudio_Id(Long audioId);

    List<AudioSegment> findAllByAudio_Practice_IdOrderBySequenceAscIdAsc(Long practiceId);

    List<AudioSegment> findAllByAudio_IdOrderBySequenceAscIdAsc(Long audioId);
}
