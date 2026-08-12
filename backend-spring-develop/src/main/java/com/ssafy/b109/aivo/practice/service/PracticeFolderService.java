package com.ssafy.b109.aivo.practice.service;

import com.ssafy.b109.aivo.global.exception.CustomException;
import com.ssafy.b109.aivo.global.exception.ErrorCode;
import com.ssafy.b109.aivo.practice.dto.*;
import com.ssafy.b109.aivo.practice.entity.PracticeFolder;
import com.ssafy.b109.aivo.practice.entity.PracticeType;
import com.ssafy.b109.aivo.practice.event.PracticeFolderDeletedEvent;
import com.ssafy.b109.aivo.practice.repository.PracticeFolderDeletionPlan;
import com.ssafy.b109.aivo.practice.repository.PracticeFolderDeletionRepository;
import com.ssafy.b109.aivo.practice.repository.projection.PracticeArchiveStatisticsProjection;
import com.ssafy.b109.aivo.practice.repository.PracticeFolderRepository;
import com.ssafy.b109.aivo.practice.repository.PracticeRepository;
import com.ssafy.b109.aivo.practice.repository.projection.PracticeFolderDetailStatisticsProjection;
import com.ssafy.b109.aivo.practice.repository.projection.PracticeFolderPracticeProjection;
import com.ssafy.b109.aivo.practice.repository.projection.PracticeScoreTrendProjection;
import com.ssafy.b109.aivo.user.entity.User;
import com.ssafy.b109.aivo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PracticeFolderService {

    private final PracticeFolderRepository practiceFolderRepository;
    private final PracticeFolderDeletionRepository practiceFolderDeletionRepository;
    private final PracticeRepository practiceRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<PracticeFolderResponse> findAll(Long userId, String type, String keyword) {
        PracticeType folderType = normalizeOptionalType(type);
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        List<PracticeFolder> folders;

        if (folderType == null) {
            folders = hasKeyword
                    ? practiceFolderRepository
                    .findAllByUserIdAndNameContainingIgnoreCaseOrderByIdDesc(
                            userId,
                            keyword.trim()
                    )
                    : practiceFolderRepository
                    .findAllByUserIdOrderByIdDesc(userId);
        } else {
            folders = hasKeyword
                    ? practiceFolderRepository
                    .findAllByUserIdAndTypeAndNameContainingIgnoreCaseOrderByIdDesc(
                            userId,
                            folderType,
                            keyword.trim()
                    )
                    : practiceFolderRepository
                    .findAllByUserIdAndTypeOrderByIdDesc(
                            userId,
                            folderType
                    );
        }

        return folders.stream()
                .map(this::toResponse)
                .toList();
    }

    public PracticeArchiveFolderListResponse findArchiveFolders(
            Long userId,
            String type,
            String keyword,
            int page
    ) {
        String normalizedType =
                normalizeArchiveType(type);

        String normalizedKeyword =
                keyword == null
                        ? ""
                        : keyword.trim();

        Pageable pageable =
                PageRequest.of(
                        Math.max(page, 0),
                        7
                );

        Page<PracticeArchiveStatisticsProjection> result =
                practiceFolderRepository
                        .findArchiveStatistics(
                                userId,
                                normalizedType,
                                normalizedKeyword,
                                pageable
                        );

        List<PracticeArchiveFolderResponse> folders =
                result.getContent()
                        .stream()
                        .map(this::toArchiveResponse)
                        .toList();

        return new PracticeArchiveFolderListResponse(
                result.getTotalElements(),
                result.getNumber(),
                result.getTotalPages(),
                result.hasNext(),
                folders
        );
    }

    public PracticeFolderResponse find(Long userId, Long folderId) {
        return toResponse(findOwnedFolder(userId, folderId));
    }

    @Transactional
    public PracticeFolderResponse create(Long userId, PracticeFolderCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.NOT_FOUND_USER
                        )
                );

        PracticeFolder folder = new PracticeFolder();

        folder.setUser(user);
        folder.setName(request.name().trim());
        folder.setDescription(
                request.description() == null ? "" : request.description().trim()
        );
        folder.setType(normalizeRequiredType(request.type()));

        PracticeFolder savedFolder = practiceFolderRepository.save(folder);

        return toResponse(savedFolder);

    }

    @Transactional
    public PracticeFolderResponse update(Long userId, Long folderId, PracticeFolderUpdateRequest request) {
        PracticeFolder folder = findOwnedFolder(userId, folderId);

        if (request.name() != null && !request.name().isBlank()) {
            folder.setName(request.name().trim());
        }
        if (request.description() != null) {
            folder.setDescription(request.description().trim());
        }

        return toResponse(folder);
    }

    @Transactional
    public void delete(Long userId, Long folderId) {

        log.info(
                "연습 폴더 삭제 요청: userId={}, folderId={}",
                userId,
                folderId
        );

        PracticeFolder folder = practiceFolderRepository
                .findByIdAndUserIdForUpdate(folderId, userId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.NOT_FOUND_PRACTICE_FOLDER
                        )
                );

        PracticeFolderDeletionPlan plan =
                practiceFolderDeletionRepository.createPlan(folder.getId());

        practiceFolderDeletionRepository.delete(plan);

        log.info(
                "연습 폴더 DB 삭제 쿼리 실행 완료: folderId={}, practiceCount={}, presentationCount={}, interviewCount={}",
                folderId,
                plan.practiceIds().size(),
                plan.presentationIds().size(),
                plan.interviewIds().size()
        );

        eventPublisher.publishEvent(
                new PracticeFolderDeletedEvent(
                        folderId,
                        plan.slideImageKeys(),
                        plan.temporaryPresentationKeys(),
                        plan.mediaObjectPaths()
                )
        );
    }

    private PracticeFolder findOwnedFolder(Long userId, Long folderId) {
        return practiceFolderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.NOT_FOUND_PRACTICE_FOLDER
                        )
                );
    }

    public PracticeFolderDetailResponse findDetail(
            Long userId,
            Long folderId
    ) {
        // 폴더 존재 여부와 사용자 소유권을 동시에 검증
        PracticeFolder folder =
                findOwnedFolder(
                        userId,
                        folderId
                );

        PracticeFolderDetailStatisticsProjection statistics =
                practiceFolderRepository
                        .findFolderDetailStatistics(
                                folderId
                        );

        return new PracticeFolderDetailResponse(
                folder.getId(),
                folder.getName(),
                folder.getDescription(),
                statistics.getAttemptCount(),
                statistics.getTotalDuration(),
                statistics.getMaxScore()
        );
    }

    public PracticeScoreTrendResponse findScoreTrend(
            Long userId,
            Long folderId
    ) {
        PracticeFolder folder = findOwnedFolder(userId, folderId);

        Pageable limit = PageRequest.of(0, 7);

        List<PracticeScoreTrendProjection> scores =
                folder.getType() == PracticeType.PRESENTATION
                        ? practiceFolderRepository
                        .findRecentPresentationScoreTrend(
                                folderId,
                                limit
                        )
                        : practiceFolderRepository
                        .findRecentInterviewScoreTrend(
                                folderId,
                                limit
                        );

        List<PracticeScoreTrendItemResponse> items =
                scores.stream()
                        .map(score ->
                                new PracticeScoreTrendItemResponse(
                                        score.getPracticeId(),
                                        score.getPracticedAt(),
                                        score.getOverallScore(),
                                        score.getVoiceScore(),
                                        score.getVideoScore(),
                                        score.getContentScore()
                                )
                        )
                        .toList();

        return new PracticeScoreTrendResponse(items);
    }

    private PracticeFolderResponse toResponse(PracticeFolder folder) {
        long practiceCount = practiceRepository.countByFolder_Id(folder.getId());
        return new PracticeFolderResponse(
                folder.getId(),
                folder.getName(),
                folder.getDescription(),
                toApiType(folder.getType()),
                practiceCount,
                practiceCount
        );
    }

    public PracticeFolderPracticeListResponse findFolderPractices(
            Long userId,
            Long folderId,
            int page,
            String sort
    ) {
        PracticeFolder folder = findOwnedFolder(
                userId,
                folderId
        );

        String sortType =
                normalizePracticeSort(sort);

        Pageable pageable =
                PageRequest.of(
                        Math.max(page, 0),
                        7
                );

        boolean isPresentation =
                folder.getType() == PracticeType.PRESENTATION;

        Page<PracticeFolderPracticeProjection> result =
                isPresentation
                        ? practiceFolderRepository.findPresentationPractices(
                                folderId,
                                sortType,
                                pageable
                        )
                        : practiceFolderRepository.findInterviewPractices(
                                folderId,
                                sortType,
                                pageable
                        );

        String practiceType = toApiType(folder.getType());

        List<PracticeFolderPracticeResponse> practices =
                result.getContent()
                        .stream()
                        .map(value ->
                                new PracticeFolderPracticeResponse(
                                        value.getPracticeId(),
                                        isPresentation ? value.getTargetId() : null,
                                        isPresentation ? null : value.getTargetId(),
                                        value.getTitle(),
                                        practiceType,
                                        value.getDurationSec(),
                                        value.getOverallScore(),
                                        value.getCreatedAt()
                                )
                        )
                        .toList();

        return new PracticeFolderPracticeListResponse(
                result.getTotalElements(),
                result.getNumber(),
                result.getTotalPages(),
                result.hasNext(),
                practices
        );
    }

    private PracticeArchiveFolderResponse
    toArchiveResponse(
            PracticeArchiveStatisticsProjection value
    ) {
        return new PracticeArchiveFolderResponse(
                value.getFolderId(),
                value.getType(),
                value.getRecentPracticeDate(),
                value.getName(),
                value.getAverageScore(),
                value.getDescription(),
                value.getAttemptCount(),
                value.getMaxScore(),
                value.getRecentScore()
        );
    }

    private String normalizeArchiveType(
            String type
    ) {
        PracticeType folderType = normalizeOptionalType(type);
        return folderType == null ? "" : folderType.name();
    }

    private PracticeType normalizeOptionalType(String type) {
        if (type == null ||
                type.isBlank() ||
                "null".equalsIgnoreCase(type.trim())) {
            return null;
        }

        if ("interview".equalsIgnoreCase(type.trim())) {
            return PracticeType.INTERVIEW;
        }

        if ("presentation".equalsIgnoreCase(type.trim())) {
            return PracticeType.PRESENTATION;
        }

        return null;
    }

    private PracticeType normalizeRequiredType(String type) {
        PracticeType folderType = normalizeOptionalType(type);
        return folderType == null
                ? PracticeType.PRESENTATION
                : folderType;
    }

    private String toApiType(PracticeType type) {
        return type.name().toLowerCase(Locale.ROOT);
    }

    private String normalizePracticeSort(
            String sort
    ) {
        if ("scoreAsc".equalsIgnoreCase(sort)) {
            return "scoreAsc";
        }

        if ("scoreDesc".equalsIgnoreCase(sort)) {
            return "scoreDesc";
        }

        return "latest";
    }
}
