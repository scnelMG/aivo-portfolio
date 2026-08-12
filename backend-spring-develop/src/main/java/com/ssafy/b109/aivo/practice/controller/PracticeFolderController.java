package com.ssafy.b109.aivo.practice.controller;

import com.ssafy.b109.aivo.practice.dto.*;
import com.ssafy.b109.aivo.practice.service.PracticeFolderService;
import com.ssafy.b109.aivo.practice.service.PracticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${API_VERSION}/practice-folders")
@RequiredArgsConstructor
public class PracticeFolderController {

    private final PracticeFolderService folderService;
    private final PracticeService practiceService;

    @GetMapping
    public ResponseEntity<List<PracticeFolderResponse>> getFolders(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(folderService.findAll(userId, type, keyword));
    }

    @GetMapping("/{folderId}")
    public ResponseEntity<PracticeFolderResponse> getFolder(
            @PathVariable Long folderId,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(folderService.find(userId, folderId));
    }

    @PostMapping
    public ResponseEntity<PracticeFolderResponse> createFolder(
            @Valid @RequestBody PracticeFolderCreateRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        folderService.create(
                                userId,
                                request
                        )
                );
    }

    @PatchMapping("/{folderId}")
    public ResponseEntity<PracticeFolderResponse> updateFolder(
            @PathVariable Long folderId,
            @Valid @RequestBody PracticeFolderUpdateRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(folderService.update(userId, folderId, request));
    }

    @DeleteMapping("/{folderId}")
    public ResponseEntity<Void> deleteFolder(
            @PathVariable Long folderId,
            @AuthenticationPrincipal Long userId
    ) {
        folderService.delete(userId, folderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{folderId}/presentation-practices")
    public ResponseEntity<PresentationPracticeListResponse>
    getPresentationPractices(
            @PathVariable Long folderId,
            @AuthenticationPrincipal Long userId
    ) {
        PresentationPracticeListResponse response =
                practiceService
                        .getPresentationPractices(
                                userId,
                                folderId
                        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/archive")
    public ResponseEntity<PracticeArchiveFolderListResponse> getArchiveFolders(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page
    ) {
        PracticeArchiveFolderListResponse response =
                folderService.findArchiveFolders(
                        userId,
                        type,
                        keyword,
                        page
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{folderId}/detail")
    public ResponseEntity<PracticeFolderDetailResponse> getFolderDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long folderId
    ) {
        return ResponseEntity.ok(
                folderService.findDetail(
                        userId, folderId
                )
        );
    }

    @GetMapping("/{folderId}/score-trend")
    public ResponseEntity<PracticeScoreTrendResponse> getFolderScoreTrend(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long folderId
    ) {
           return ResponseEntity.ok(
                   folderService.findScoreTrend(
                           userId, folderId
                   )
           );
    }

    @GetMapping("/{folderId}/practices")
    public ResponseEntity<PracticeFolderPracticeListResponse>
    getFolderPractices(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long folderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        PracticeFolderPracticeListResponse response =
                folderService.findFolderPractices(
                        userId,
                        folderId,
                        page,
                        sort
                );

        return ResponseEntity.ok(response);
    }

}
