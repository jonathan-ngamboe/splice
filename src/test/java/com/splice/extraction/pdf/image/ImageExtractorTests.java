package com.splice.extraction.pdf.image;

import com.splice.extraction.spi.AssetStorage;
import com.splice.model.document.content.ImageContent;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class ImageExtractorTests {

    @Mock
    private AssetStorage mockStorage;

    private ImageExtractor extractor;

    @BeforeEach
    void setup() throws IOException {
        extractor = new ImageExtractor(mockStorage);

        org.mockito.Mockito.lenient().when(mockStorage.store(any(), anyString()))
                .thenReturn("s3://fake-bucket/image.png");
    }

    @Test
    @DisplayName("Should extract visible images with correct WEB coordinates (Top-Left origin)")
    void shouldExtractValidImages() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(0, 0, 500, 500));
            doc.addPage(page);

            BufferedImage bufferedImage = createDummyImage(100, 100);

            injectImageIntoPage(doc, page, bufferedImage, 50, 100);

            var results = extractor.extract(page, 1);

            assertEquals(1, results.size(), "Should find exactly one image");

            var element = results.getFirst();
            var bbox = element.location().bbox();

            assertEquals(50, bbox.x(), 0.1, "X coordinate should match");
            assertEquals(100, bbox.width(), 0.1, "Width should match");
            assertEquals(100, bbox.height(), 0.1, "Height should match");

            assertEquals(300, bbox.y(), 0.1, "Y coordinate should be converted to Top-Left origin");

            assertInstanceOf(ImageContent.class, element.content());
            assertEquals("s3://fake-bucket/image.png", ((ImageContent) element.content()).imagePath());
        }
    }

    @Test
    @DisplayName("Should ignore small artifacts (filtering logic)")
    void shouldIgnoreTinyArtifacts() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(500, 500));
            doc.addPage(page);

            BufferedImage tinyImage = createDummyImage(1, 1);
            BufferedImage validImage = createDummyImage(60, 60);

            injectImageIntoPage(doc, page, tinyImage, 0, 0);
            injectImageIntoPage(doc, page, validImage, 100, 100);

            var results = extractor.extract(page, 1);

            assertEquals(1, results.size(), "Should only keep images larger than threshold");

            assertEquals(60, results.getFirst().location().bbox().width(), 0.1);
        }
    }

    @Test
    @DisplayName("Should return empty list for page without images")
    void shouldReturnEmptyForTextOnlyPage() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            var results = extractor.extract(page, 1);

            assertTrue(results.isEmpty());
        }
    }

    @Test
    @DisplayName("Should handle null page safely")
    void shouldHandleNullPage() throws IOException {
        var results = extractor.extract(null, 0);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    private void injectImageIntoPage(PDDocument doc, PDPage page, BufferedImage img, float x, float y) throws IOException {
        PDImageXObject pdImage = LosslessFactory.createFromImage(doc, img);

        try (PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            contentStream.drawImage(pdImage, x, y);
        }
    }

    private BufferedImage createDummyImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }
}