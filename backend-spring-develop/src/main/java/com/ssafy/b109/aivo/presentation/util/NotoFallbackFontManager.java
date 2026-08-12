package com.ssafy.b109.aivo.presentation.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.common.usermodel.fonts.FontInfo;
import org.apache.poi.sl.draw.DrawFontManagerDefault;

import java.awt.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class NotoFallbackFontManager extends DrawFontManagerDefault {
    private static final String FALLBACK_FONT =
            "Noto Sans KR";

    private static final FontInfo FALLBACK_FONT_INFO =
            () -> FALLBACK_FONT;

    private static final Set<String> LOGICAL_FONTS = Set.of(
            Font.DIALOG,
            Font.DIALOG_INPUT,
            Font.SANS_SERIF,
            Font.SERIF,
            Font.MONOSPACED
    );

    private final Set<String> loggedMissingFonts =
            ConcurrentHashMap.newKeySet();

    @Override
    public FontInfo getFallbackFont(
            Graphics2D graphics,
            FontInfo fontInfo
    ) {
        return FALLBACK_FONT_INFO;
    }

    @Override
    public Font createAWTFont(
            Graphics2D graphics,
            FontInfo fontInfo,
            double fontSize,
            boolean bold,
            boolean italic
    ) {
        int style = Font.PLAIN;

        if (bold) {
            style |= Font.BOLD;
        }

        if (italic) {
            style |= Font.ITALIC;
        }

        String requestedFont =
                fontInfo == null
                        ? null
                        : fontInfo.getTypeface();

        if (requestedFont != null &&
            !requestedFont.isBlank()) {

            Font resolvedFont =
                    new Font(
                            requestedFont,
                            style,
                            12
                    );

            if (isAvailable(
                    requestedFont,
                    resolvedFont
            )) {
                return resolvedFont.deriveFont(
                        (float) fontSize
                );
            }

            if (loggedMissingFonts.add(requestedFont)) {
                log.info(
                        "PPT 폰트 대체: requestedFont={}, fallbackFont={}",
                        requestedFont,
                        FALLBACK_FONT
                );
            }
        }

        return new Font(
                FALLBACK_FONT,
                style,
                12
        ).deriveFont(
                (float) fontSize
        );
    }

    private boolean isAvailable(
            String requestedFont,
            Font resolvedFont
    ) {
        boolean logicalFont =
                LOGICAL_FONTS.stream()
                        .anyMatch(font ->
                                font.equalsIgnoreCase(
                                        requestedFont
                                )
                        );

        if (logicalFont) {
            return true;
        }

        // 존재하지 않는 폰트를 new Font()로 생성하면
        // Java가 Dialog 폰트로 대체한다.
        return !Font.DIALOG.equalsIgnoreCase(
                resolvedFont.getFamily()
        );
    }
}
