package com.ssafy.b109.aivo.practice.repository;

import java.util.List;
import java.util.Set;

public record PracticeFolderDeletionPlan(
        Long folderId,
        List<Long> practiceIds,
        List<Long> presentationIds,
        List<Long> interviewIds,
        List<Long> totalFeedbackIds,
        List<Long> audioIds,
        List<Long> presentationSlideIds,
        List<Long> presentationQuestionIds,
        List<Long> interviewQuestionIds,
        Set<String> slideImageKeys,
        Set<String> temporaryPresentationKeys,
        Set<String> mediaObjectPaths
) {
}
