package com.ssafy.b109.aivo.practice.event;

import java.util.Set;

public record PracticeFolderDeletedEvent(
        Long folderId,
        Set<String> slideImageKeys,
        Set<String> temporaryPresentationKeys,
        Set<String> mediaObjectPaths
) {
    public PracticeFolderDeletedEvent {
        slideImageKeys = Set.copyOf(slideImageKeys);
        temporaryPresentationKeys = Set.copyOf(temporaryPresentationKeys);
        mediaObjectPaths = Set.copyOf(mediaObjectPaths);
    }
}
