package com.ssafy.b109.aivo.practice.dto;

public record UserSpeechTrendResponse(
        Integer averageSpeechSpeed,
        Integer earlySpeechSpeed,
        Integer lateSpeechSpeed,
        Double silenceLate
) {
}
