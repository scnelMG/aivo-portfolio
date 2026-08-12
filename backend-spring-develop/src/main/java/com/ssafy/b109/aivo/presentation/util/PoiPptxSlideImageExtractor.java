package com.ssafy.b109.aivo.presentation.util;

import com.ssafy.b109.aivo.global.exception.CustomException;
import com.ssafy.b109.aivo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.sl.draw.Drawable;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class PoiPptxSlideImageExtractor implements SlideImageExtractor{

    private static final double SCALE = 1.1;
    private static final float JPEG_QUALITY = 0.7f;
    private static final Pattern EMPTY_PRESET_DASH = Pattern.compile(
            "(?s)<a:prstDash(?![^>]*\\bval\\s*=)[^>]*/>|"
                    + "<a:prstDash(?![^>]*\\bval\\s*=)[^>]*>\\s*</a:prstDash>"
    );
    private static final Map<String, String> FONT_FALLBACK = Map.of(
            "*",
            "Noto Sans KR"
    );

    private final JpegSlideImageEncoder jpegSlideImageEncoder;
    private final NotoFallbackFontManager fontManager = new NotoFallbackFontManager();

    @Override
    public List<ExtractedSlideImage> extract(InputStream inputStream) {

        try {
            // 일부 PPTX가 <a:prstDash/>처럼 val 없는 선 스타일을 포함하면
            // Apache POI 렌더링 과정에서 NPE가 발생하므로 로딩 전에 보정한다.
            byte[] normalizedPptx = normalizePptx(inputStream);

            try (XMLSlideShow slideShow = new XMLSlideShow(
                    new ByteArrayInputStream(normalizedPptx))) {
            List<XSLFSlide> slides = slideShow.getSlides();

            log.info("PPTX 슬라이드 이미지 압축 대상 확인: slideCount={}", slides.size());

            if (slides.isEmpty()) {
                throw new CustomException(
                        ErrorCode.EMPTY_PRESENTATION_SLIDES
                );
            }

            Dimension pageSize = slideShow.getPageSize();

            int imageWidth =
                    (int) Math.ceil(pageSize.getWidth() * SCALE);

            int imageHeight =
                    (int) Math.ceil(pageSize.getHeight() * SCALE);

            List<ExtractedSlideImage> results =
                    new ArrayList<>(slides.size());

            for (int index = 0; index < slides.size(); index++) {
                int slideNumber = index + 1;
                log.info("PPTX 슬라이드 이미지 압축 시작: slideNumber={}", slideNumber);

                byte[] imageData = renderToJpegBytes(
                        slides.get(index),
                        imageWidth,
                        imageHeight
                );

                log.info(
                        "PPTX 슬라이드 이미지 압축 완료: slideNumber={}, bytes={}",
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
            }

        } catch (CustomException e) {
            throw e;
        } catch (IOException e) {
            throw new CustomException(ErrorCode.PRESENTATION_SLIDE_CONVERSION_FAILED);
        }
    }

    private byte[] normalizePptx(InputStream inputStream) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {

            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                ZipEntry normalizedEntry = new ZipEntry(entry.getName());
                if (entry.getTime() >= 0) {
                    normalizedEntry.setTime(entry.getTime());
                }
                zipOutputStream.putNextEntry(normalizedEntry);

                byte[] content = zipInputStream.readAllBytes();
                if (entry.getName().endsWith(".xml")) {
                    String xml = new String(content, java.nio.charset.StandardCharsets.UTF_8);
                    content = EMPTY_PRESET_DASH.matcher(xml)
                            .replaceAll("")
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                }

                zipOutputStream.write(content);
                zipOutputStream.closeEntry();
            }

            zipOutputStream.finish();
            return outputStream.toByteArray();
        }
    }

    private byte[] renderToJpegBytes(
            XSLFSlide slide,
            int imageWidth,
            int imageHeight
    ) {
        BufferedImage image = new BufferedImage(
                imageWidth,
                imageHeight,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D graphics =
                image.createGraphics();

        try {
            applyRenderingHints(graphics);

            graphics.setRenderingHint(
                    Drawable.TEXT_RENDERING_MODE,
                    Drawable.TEXT_AS_SHAPES
            );

            graphics.setRenderingHint(
                    Drawable.FONT_HANDLER,
                    fontManager
            );

            graphics.setColor(Color.WHITE);
            graphics.fillRect(
                    0,
                    0,
                    imageWidth,
                    imageHeight
            );

            graphics.scale(SCALE, SCALE);
            slide.draw(graphics);

        } finally {
            graphics.dispose();
        }

        return jpegSlideImageEncoder.encode(image);
    }

    private void applyRenderingHints(Graphics2D graphics) {
        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        graphics.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );

        graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
        );

        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );
    }

    @Override
    public boolean supports(String extension) {
        return "pptx".equalsIgnoreCase(extension);
    }
}

