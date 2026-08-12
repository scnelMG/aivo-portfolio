package com.ssafy.b109.aivo.rabbitmq.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record AudioSttSegment(
        String text,
        @JsonAlias("start")
        Float timestampSt,
        @JsonAlias("end")
        Float timestampEnd
) {
}
