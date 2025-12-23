package com.splice.service.pdf;

import com.splice.model.ImageContent;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ImageExtractorTests {

    private final ImageExtractor extractor = new ImageExtractor();

    @Test
    @DisplayName("Should extract visible images with correct location")
    void shouldExtractValidImages() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            BufferedImage bufferedImage = createDummyImage(100, 100);

            injectImageIntoPage(doc, page, bufferedImage, 50, 100);

            var results = extractor.extract(page);

            assertEquals(1, results.size(), "Should find exactly one image");

            var element = results.getFirst();
            var location = element.location().bbox();

            assertEquals(50, location.getLowerLeftX(), 0.1, "X coordinate mismatch");
            assertEquals(100, location.getLowerLeftY(), 0.1, "Y coordinate mismatch");
            assertEquals(100, location.getWidth(), 0.1, "Width mismatch");

            assertInstanceOf(ImageContent.class, element.content());
        }
    }

    @Test
    @DisplayName("Should ignore invisible or very small images (Artifacts)")
    void shouldIgnoreTinyArtifacts() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            BufferedImage tinyImage = createDummyImage(1, 1);
            BufferedImage validImage = createDummyImage(50, 50);

            injectImageIntoPage(doc, page, tinyImage, 0, 0);
            injectImageIntoPage(doc, page, validImage, 100, 100);

            var results = extractor.extract(page);

            assertEquals(1, results.size(), "Should verify filtering logic (ignore < 10px)");
            assertEquals(100, results.getFirst().location().bbox().getLowerLeftX(), 0.1);
        }
    }

    @Test
    @DisplayName("Should return empty list for page without images")
    void shouldReturnEmptyForTextOnlyPage() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            var results = extractor.extract(page);

            assertTrue(results.isEmpty());
        }
    }

    @Test
    @DisplayName("Should handle null page safely")
    void shouldHandleNullPage() {
        var results = extractor.extract(null);
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