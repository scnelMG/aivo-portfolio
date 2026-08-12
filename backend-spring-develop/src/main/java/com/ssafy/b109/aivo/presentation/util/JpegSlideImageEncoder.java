package com.ssafy.b109.aivo.presentation.util;

import com.ssafy.b109.aivo.global.exception.CustomException;
import com.ssafy.b109.aivo.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@Component
public class JpegSlideImageEncoder {

    private static final float JPEG_QUALITY =
            0.9f;

    public byte[] encode(BufferedImage image) {
        if (image == null) {
            throw new CustomException(
                    ErrorCode.PRESENTATION_SLIDE_IMAGE_CREATE_FAILED
            );
        }

        Iterator<ImageWriter> writers =
                ImageIO.getImageWritersByFormatName(
                        "jpeg"
                );

        if (!writers.hasNext()) {
            throw new CustomException(
                    ErrorCode.PRESENTATION_IMAGE_WRITER_NOT_FOUND
            );
        }

        ImageWriter writer = writers.next();

        try (
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(
                                outputStream
                        )
        ) {
            ImageWriteParam parameter = writer.getDefaultWriteParam();

            parameter.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

            parameter.setCompressionQuality(JPEG_QUALITY);

            writer.setOutput(imageOutputStream);

            writer.write(
                    null,
                    new IIOImage(
                            image,
                            null,
                            null
                    ),
                    parameter
            );

            imageOutputStream.flush();

            byte[] imageData = outputStream.toByteArray();

            if (imageData.length == 0) {
                throw new CustomException(
                        ErrorCode.PRESENTATION_SLIDE_IMAGE_CREATE_FAILED
                );
            }

            return imageData;

        } catch (IOException e) {
            throw new CustomException(
                    ErrorCode.PRESENTATION_SLIDE_IMAGE_CREATE_FAILED
            );
        } finally {
            writer.dispose();
        }
    }
}
