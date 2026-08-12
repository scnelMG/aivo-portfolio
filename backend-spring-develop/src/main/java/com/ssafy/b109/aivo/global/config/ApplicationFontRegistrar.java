package com.ssafy.b109.aivo.global.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.awt.*;
import java.io.InputStream;

@Component
@Slf4j
public class ApplicationFontRegistrar {
    private static final List<String> FONT_PATHS = List.of(
                    "/fonts/NotoSansKR-Regular.ttf",
                    "/fonts/NotoSansKR-Bold.ttf"
            );

    @PostConstruct
    public void registerFonts() {
        FONT_PATHS.forEach(
                this::registerFont
        );
    }

    private void registerFont(String path) {
        try (InputStream inputStream =
                     getClass().getResourceAsStream(path)) {

            if (inputStream == null) {
                throw new IllegalStateException(
                        "폰트 파일을 찾을 수 없습니다: " + path
                );
            }

            Font font = Font.createFont(
                    Font.TRUETYPE_FONT,
                    inputStream
            );

            boolean registered = GraphicsEnvironment
                            .getLocalGraphicsEnvironment()
                            .registerFont(font);

            log.info(
                    "폰트 등록: path={}, family={}, registered={}",
                    path,
                    font.getFamily(),
                    registered
            );

        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException(
                    "폰트 등록 실패: " + path,
                    e
            );
        }
    }
}
