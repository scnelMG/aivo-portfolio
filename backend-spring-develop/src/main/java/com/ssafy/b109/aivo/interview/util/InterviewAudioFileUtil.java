package com.ssafy.b109.aivo.interview.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.b109.aivo.interview.dto.AudioAnalysisResult;

import java.util.Map;

public final class InterviewAudioFileUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private InterviewAudioFileUtil() {
    }

    public static String analysisMetadata(AudioAnalysisResult result) {
        try {
            return OBJECT_MAPPER.writeValueAsString(Map.of(
                    "fillerCount", result.fillerCount(),
                    "silenceDetected", result.silenceDetected(),
                    "stutterDetected", result.stutterDetected(),
                    "silenceDurationMs", result.silenceDurationMs(),
                    "averageWpm", result.averageWpm(),
                    "fillerEvents", result.fillerEvents() == null ? java.util.List.of() : result.fillerEvents()
            ));
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }
}
