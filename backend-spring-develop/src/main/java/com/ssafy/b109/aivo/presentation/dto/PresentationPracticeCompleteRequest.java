package com.ssafy.b109.aivo.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record PresentationPracticeCompleteRequest(
        @NotNull
        @Positive
        Long durationMs,

        @NotNull
        List<@Valid PresentationTextRequest> text,

        @NotNull
        @Valid
        PresentationNonverbalRequest nonverbal
) {
        public record PresentationTextRequest(
                @NotNull
                @Positive
                Integer page,

                @NotNull
                @PositiveOrZero
                Long timestamp,

                @NotBlank
                String content
        ) {
        }

        public record PresentationNonverbalRequest(
                @NotNull
                @PositiveOrZero
                Integer gazeDeviationCount,

                @NotNull
                @DecimalMin("0.0")
                @DecimalMax("100.0")
                Double postureTiltPercent,

                @NotNull
                @PositiveOrZero
                Integer sampleCount,

                @NotNull
                List<@Valid GazeEventRequest> gazeEvents,

                @NotNull
                List<@Valid TiltBucketRequest> tiltBuckets
        ) {
        }

        public record GazeEventRequest(
                @NotNull
                @PositiveOrZero
                Double atSec
        ) {
        }

        public record TiltBucketRequest(
                @NotNull
                @PositiveOrZero
                Integer startSec,

                @NotNull
                @PositiveOrZero
                Integer endSec,

                @NotNull
                @DecimalMin("0.0")
                @DecimalMax("100.0")
                Double tiltPct
        ) {
        }
}