package com.ssafy.b109.aivo.presentation.util;

import com.ssafy.b109.aivo.global.exception.CustomException;
import com.ssafy.b109.aivo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class PdfBoxSlideImageExtractor implements SlideImageExtractor {

    /*
     * OCR 정확도와 이미지 크기의 균형을 고려한 초기값.
     * DPI를 높이면 이미지 품질과 메모리 사용량이 함께 증가한다.
     */
    private static final float IMAGE_DPI = 200F;

    private final JpegSlideImageEncoder jpegSlideImageEncoder;


    @Override
    public boolean supports(String extension) {
        return "pdf".equalsIgnoreCase(extension);
    }

    @Override
    public List<ExtractedSlideImage> extract(InputStream inputStream) {

        try (RandomAccessReadBuffer buffer = new RandomAccessReadBuffer(inputStream);
             PDDocument document =
                     Loader.loadPDF(buffer)) {

            int pageCount = document.getNumberOfPages();
            log.info("PDF 슬라이드 이미지 압축 대상 확인: pageCount={}", pageCount);

            if (pageCount == 0) {
                throw new CustomException(ErrorCode.EMPTY_PRESENTATION_SLIDES);
            }

            PDFRenderer renderer =
                    new PDFRenderer(document);

            List<ExtractedSlideImage> results =
                    new ArrayList<>(pageCount);

            for (int pageIndex = 0;
                 pageIndex < pageCount;
                 pageIndex++) {

                int slideNumber = pageIndex + 1;
                log.info("PDF 슬라이드 이미지 압축 시작: slideNumber={}", slideNumber);

                BufferedImage image =
                        renderer.renderImageWithDPI(
                                pageIndex,
                                IMAGE_DPI,
                                ImageType.RGB
                        );

                byte[] imageData = jpegSlideImageEncoder.encode(image);
                        ;
                log.info(
                        "PDF 슬라이드 이미지 압축 완료: slideNumber={}, bytes={}",
                        slideNumber,
                        imageData.length
                );

                results.add(
                        new ExtractedSlideImage(
                                slideNumber,
                                imageData
                        )
                );
            }

            return results;

        } catch (CustomException e) {
            throw e;

        } catch (IOException e) {
            throw new CustomException(ErrorCode.PRESENTATION_SLIDE_CONVERSION_FAILED);
        }
    }

}
