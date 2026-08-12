package com.ssafy.b109.aivo.practice.event;

import com.ssafy.b109.aivo.portfolio.util.S3PortfolioUploader;
import com.ssafy.b109.aivo.presentation.util.S3PresentationDeleter;
import com.ssafy.b109.aivo.presentation.util.S3SlideImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PracticeFolderDeletedEventListener {

    private final S3SlideImageStorage slideImageStorage;
    private final S3PresentationDeleter presentationDeleter;
    private final S3PortfolioUploader mediaStorage;

    @Async("presentationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PracticeFolderDeletedEvent event) {
        log.info(
                "연습 폴더 DB 삭제 커밋 완료, S3 정리 시작: folderId={}, slideCount={}, temporaryFileCount={}, mediaCount={}",
                event.folderId(),
                event.slideImageKeys().size(),
                event.temporaryPresentationKeys().size(),
                event.mediaObjectPaths().size()
        );

        event.slideImageKeys().forEach(this::deleteSlideImage);
        event.temporaryPresentationKeys().forEach(this::deletePresentationFile);
        event.mediaObjectPaths().forEach(this::deleteMedia);

        log.info(
                "연습 폴더 S3 정리 작업 종료: folderId={}",
                event.folderId()
        );
    }

    private void deleteSlideImage(String objectKey) {
        try {
            slideImageStorage.delete(objectKey);
        } catch (Exception e) {
            log.error("폴더 삭제 후 슬라이드 이미지 S3 정리 실패: objectKey={}", objectKey, e);
        }
    }

    private void deletePresentationFile(String objectKey) {
        try {
            presentationDeleter.delete(objectKey);
        } catch (Exception e) {
            log.error("폴더 삭제 후 발표 원본 S3 정리 실패: objectKey={}", objectKey, e);
        }
    }

    private void deleteMedia(String objectPath) {
        try {
            mediaStorage.delete(objectPath);
        } catch (Exception e) {
            log.error("폴더 삭제 후 연습 미디어 S3 정리 실패: objectPath={}", objectPath, e);
        }
    }
}
