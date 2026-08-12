package com.ssafy.b109.aivo.rabbitmq.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.b109.aivo.global.exception.CustomException;
import com.ssafy.b109.aivo.global.exception.ErrorCode;
import com.ssafy.b109.aivo.media.entity.Audio;
import com.ssafy.b109.aivo.media.entity.AudioSegment;
import com.ssafy.b109.aivo.media.entity.AudioStt;
import com.ssafy.b109.aivo.media.repository.AudioRepository;
import com.ssafy.b109.aivo.media.repository.AudioSegmentRepository;
import com.ssafy.b109.aivo.media.repository.AudioSttRepository;
import com.ssafy.b109.aivo.rabbitmq.dto.AudioAnalysisCompletedMessage;
import com.ssafy.b109.aivo.rabbitmq.dto.AudioSttSegment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class AudioAnalysisResultService {

    private final AudioRepository audioRepository;
    private final AudioSttRepository audioSttRepository;
    private final AudioSegmentRepository audioSegmentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void saveAudioStt(AudioAnalysisCompletedMessage message) {
        Audio audio = audioRepository.findById(message.audioId())
                .or(() -> audioRepository.findByPracticeId(message.practiceId()))
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_AUDIO_ANALYSIS_REQUEST));

        if(!Objects.equals(message.practiceId(), audio.getPractice().getId())){
            throw new CustomException(ErrorCode.INVALID_AUDIO_ANALYSIS_REQUEST);
        }

        AudioStt audioStt = audioSttRepository.findFirstByAudioIdOrderByIdDesc(audio.getId())
                .orElseGet(AudioStt::new);

        audioStt.setAudio(audio);
        audioStt.setContent(toJson(message));
        audioStt.setCreatedAt(LocalDateTime.now());

        audioSttRepository.save(audioStt);
        replaceAudioSegments(audio, message.segments());
    }

    private void replaceAudioSegments(
            Audio audio,
            List<AudioSttSegment> segments
    ) {
        audioSegmentRepository.deleteAllByAudio_Id(
                audio.getId()
        );

        if (segments == null || segments.isEmpty()) {
            return;
        }

        LocalDateTime now =
                LocalDateTime.now();

        List<AudioSegment> audioSegments =
                IntStream.range(
                                0,
                                segments.size()
                        )
                        .mapToObj(index ->
                                createAudioSegment(
                                        audio,
                                        segments.get(index),
                                        index,
                                        now
                                )
                        )
                        .toList();

        audioSegmentRepository.saveAll(
                audioSegments
        );
    }

    private AudioSegment createAudioSegment(
            Audio audio,
            AudioSttSegment segment,
            int sequence,
            LocalDateTime createdAt
    ) {
        if (segment.timestampSt() == null
            || segment.timestampEnd() == null) {
            throw new CustomException(
                    ErrorCode.AUDIO_STT_FAILED
            );
        }

        double startSec = segment.timestampSt().doubleValue();

        double endSec = segment.timestampEnd().doubleValue();

        if (startSec < 0 || endSec < startSec) {
            throw new CustomException(
                    ErrorCode.AUDIO_STT_FAILED
            );
        }

        AudioSegment audioSegment = new AudioSegment();

        audioSegment.setAudio(audio);
        audioSegment.setSequence(sequence);
        audioSegment.setStartSec(startSec);
        audioSegment.setEndSec(endSec);
        audioSegment.setStartTimeMs(
                Math.round(startSec * 1000.0)
        );
        audioSegment.setEndTimeMs(
                Math.round(endSec * 1000.0)
        );
        audioSegment.setText(
                segment.text() == null
                        ? ""
                        : segment.text()
        );
        audioSegment.setCreatedAt(createdAt);

        return audioSegment;
    }

    private String toJson(AudioAnalysisCompletedMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new CustomException(ErrorCode.AUDIO_STT_FAILED);
        }
    }
}
