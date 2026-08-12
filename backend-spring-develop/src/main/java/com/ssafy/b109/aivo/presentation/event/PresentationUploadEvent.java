package com.ssafy.b109.aivo.presentation.event;

public record PresentationUploadEvent(
        Long userId,
        Long presentationId
) {
}
