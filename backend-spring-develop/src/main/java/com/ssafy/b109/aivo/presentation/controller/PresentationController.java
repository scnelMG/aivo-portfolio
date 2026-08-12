package com.ssafy.b109.aivo.presentation.controller;

import com.ssafy.b109.aivo.practice.entity.Practice;
import com.ssafy.b109.aivo.practice.repository.PracticeRepository;
import com.ssafy.b109.aivo.presentation.dto.*;
import com.ssafy.b109.aivo.presentation.entity.Presentation;
import com.ssafy.b109.aivo.presentation.entity.PresentationReportJob;
import com.ssafy.b109.aivo.presentation.entity.PresentationReportJobStatus;
import com.ssafy.b109.aivo.presentation.repository.PresentationReportJobRepository;
import com.ssafy.b109.aivo.presentation.repository.PresentationRepository;
import com.ssafy.b109.aivo.presentation.service.PresentationPracticeService;
import com.ssafy.b109.aivo.presentation.service.PresentationReportService;
import com.ssafy.b109.aivo.presentation.service.PresentationReportJobStatusService;
import com.ssafy.b109.aivo.presentation.service.PresentationService;
import com.ssafy.b109.aivo.presentation.service.PresentationSlideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.net.URI;

@RestController
@RequestMapping("${API_VERSION}")
@RequiredArgsConstructor
@Slf4j
public class PresentationController {

    private final PresentationService presentationService;
    private final PresentationSlideService presentationSlideService;
    private final PresentationPracticeService presentationPracticeService;
    private final PresentationReportService presentationReportService;
    private final PresentationRepository presentationRepository;
    private final PracticeRepository practiceRepository;
    private final PresentationReportJobRepository presentationReportJobRepository;
    private final PresentationReportJobStatusService presentationReportJobStatusService;

    @PostMapping("/presentations/{presentationId}/presentation-questions/generate")
    public ResponseEntity<List<AudienceQuestionResponse>> generateAudienceQuestion(
            @PathVariable Long presentationId,
            @AuthenticationPrincipal Long userId,
            @RequestBody List<AudienceQuestionRequest> requests
    ){
        List<AudienceQuestionResponse> audienceQuestionResponses =
                presentationService.generateAudienceQuestion(presentationId, userId, requests);

        return ResponseEntity.status(HttpStatus.CREATED).body(audienceQuestionResponses);
    }

    @GetMapping("/presentations/{presentationId}/presentation-questions")
    public ResponseEntity<List<AudienceQuestionResponse>> getAudienceQuestions(
            @PathVariable Long presentationId,
            @AuthenticationPrincipal Long userId
    ){
        List<AudienceQuestionResponse> audienceQuestionResponses =
                presentationService.getAudienceQuestions(presentationId, userId);

        return ResponseEntity.ok(audienceQuestionResponses);
    }

    @PostMapping("/presentation-questions/{questionId}/answers")
    public ResponseEntity<Void> saveAudienceQuestionAnswers(
            @PathVariable Long questionId,
            @AuthenticationPrincipal Long userId,
            @RequestBody AudienceQuestionAnswerRequest request
    ){
        presentationService.saveAudienceQuestionAnswers(questionId, userId, request);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/presentation-questions/{questionId}/feedback")
    public ResponseEntity<PresentationQuestionFeedbackResponse> getAudienceQuestionFeedback(
            @PathVariable Long questionId,
            @AuthenticationPrincipal Long userId
    ){
        PresentationQuestionFeedbackResponse response =
                presentationService.getAudienceQuestionFeedback(questionId, userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/presentations/{presentationId}/presentation-question-feedbacks")
    public ResponseEntity<List<PresentationQuestionFeedbackResponse>> getAudienceQuestionFeedbacks(
            @PathVariable Long presentationId,
            @AuthenticationPrincipal Long userId
    ){
        List<PresentationQuestionFeedbackResponse> responses =
                presentationService.getAudienceQuestionFeedbacks(presentationId, userId);

        return ResponseEntity.ok(responses);
    }



    @PostMapping(value = "/presentations",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<PresentationFileUploadResponse> create(
            @RequestPart("request") @Valid PresentationCreateRequest request,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Long userId
    )
    {

        PresentationFileUploadResponse response = presentationService.createWithFile(userId, request, file);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping(
            "/presentations/{presentationId}/slides/{slideNumber}/image"
    )
    public ResponseEntity<Void> getSlideImage(
            @PathVariable Long presentationId,
            @PathVariable Integer slideNumber,
            @AuthenticationPrincipal Long userId
    ) {
        URI imageUrl =
                presentationSlideService
                        .getImageUrlForUser(
                                userId,
                                presentationId,
                                slideNumber
                        );

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(imageUrl)
                .build();
    }

    @GetMapping(
            "/presentations/{presentationId}/slides/image"
    )
    public ResponseEntity<PresentationSlideImagesResponse>
    getSlideImages(
            @PathVariable Long presentationId,
            @AuthenticationPrincipal Long userId
    ) {
        PresentationSlideImagesResponse response =
                presentationSlideService
                        .getSlideImagesForUser(
                                userId,
                                presentationId
                        );

        return ResponseEntity.ok(response);
    }

    @PutMapping(
            value = "/presentations/{presentationId}/presentation-document",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Void> reupload(
            @PathVariable Long presentationId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Long userId
    ) {
        presentationService.reupload(userId, presentationId, file);

        return ResponseEntity.accepted().build();
    }

    @GetMapping("/presentations/{presentationId}/status")
    public ResponseEntity<PresentationStatusResponse> getPresentationAnalysisStatus(
            @PathVariable Long presentationId,
            @AuthenticationPrincipal Long userId
    ) {

        return ResponseEntity.ok(
                presentationService.getStatus(
                        userId,
                        presentationId
                )
        );
    }

    @GetMapping(
            "/presentations/{presentationId}/slides"
    )
    public ResponseEntity<PresentationSlidesResponse>
    getSlides(
            @PathVariable Long presentationId,
            @AuthenticationPrincipal Long userId
    ) {
        PresentationSlidesResponse response =
                presentationSlideService
                        .getSlidesForUser(
                                userId,
                                presentationId
                        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping(
            "/presentations/{presentationId}/slides/descriptions"
    )
    public ResponseEntity<Void> updateSlideDescriptions(
            @PathVariable Long presentationId,
            @RequestBody @Valid
            PresentationSlideDescriptionsUpdateRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        presentationSlideService.updateDescriptions(
                userId,
                presentationId,
                request
        );

        return ResponseEntity.noContent().build();
    }

    @PostMapping("presentations/{presentationId}/start")
    public ResponseEntity<PresentationPracticeStartResponse>
    start(
            @PathVariable Long presentationId,
            @AuthenticationPrincipal Long userId
    ) {
        PresentationPracticeStartResponse response =
                presentationPracticeService.start(
                        userId,
                        presentationId
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/presentations/{presentationId}/slide-events")
    public ResponseEntity<Void> createSlideEvent(
            @PathVariable Long presentationId,
            @RequestBody @Valid SlideEventCreateRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        presentationPracticeService.createSlideEvent(
                userId,
                presentationId,
                request
        );

        return ResponseEntity.noContent().build();
    }


    @PostMapping(value = "/presentations/{presentationId}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> presentationComplete(
            @PathVariable Long presentationId,
            @RequestPart(value = "request", required = false)
            @Valid PresentationPracticeCompleteRequest request,
            @RequestPart(value = "audio", required = false) MultipartFile audio,
            @RequestPart(value = "video", required = false) MultipartFile video,
            @AuthenticationPrincipal Long userId
    ){
        Presentation curPresentation = presentationRepository.findById(presentationId)
                .orElseThrow(()-> new RuntimeException("해당 발표를 찾을 수 없습니다."));
        Practice curPractice = practiceRepository.findByPresentationId(presentationId)
                .orElseThrow(()-> new RuntimeException("해당 연습을 찾을 수 없습니다."));
        PresentationReportJob presentationReportJob = new PresentationReportJob();
        presentationReportJob.setPresentation(curPresentation);
        presentationReportJob.setPractice(curPractice);
        presentationReportJob.setStatus(PresentationReportJobStatus.PENDING);
        LocalDateTime now = LocalDateTime.now();
        presentationReportJob.setCreatedAt(now);
        presentationReportJob.setUpdatedAt(now);
        presentationReportJobRepository.saveAndFlush(presentationReportJob);

        try {
            presentationPracticeService.complete(
                    userId,
                    presentationId,
                    request
            );

            presentationService.upload(presentationId, audio, video, userId);
        } catch (Exception exception) {
            presentationReportJobStatusService.markFailed(curPractice.getId(), exception);
            throw exception;
        }

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/presentations/reuse")
    public ResponseEntity<PresentationFileUploadResponse>
    reuse(
            @RequestBody @Valid
            PresentationReuseRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        PresentationFileUploadResponse response =
                presentationService.createFromExisting(
                        userId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/presentations/{presentationId}/presentation-report")
    public ResponseEntity<PresentationReportResponse> getPresentationReport(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long presentationId
    ) {
        return ResponseEntity.ok(
                presentationReportService.getReport(
                        userId,
                        presentationId
                )
        );
    }

    @GetMapping("/presentations/{presentationId}/report-job/status")
    public ResponseEntity<PresentationReportJobStatusResponse> getPresentationReportJobStatus(
            @PathVariable Long presentationId,
            @AuthenticationPrincipal Long userId
    ) {
        Practice practice = practiceRepository.findByPresentation_IdAndFolder_User_Id(
                        presentationId,
                        userId
                )
                .orElseThrow(() -> new RuntimeException("해당 연습을 찾을 수 없습니다."));

        PresentationReportJob job = presentationReportJobRepository.findByPractice(practice)
                .orElseThrow(() -> new RuntimeException("presentationReportJob을 찾을 수 없습니다."));

        return ResponseEntity.ok(
                PresentationReportJobStatusResponse.of(
                        presentationId,
                        practice.getId(),
                        job
                )
        );
    }
}
