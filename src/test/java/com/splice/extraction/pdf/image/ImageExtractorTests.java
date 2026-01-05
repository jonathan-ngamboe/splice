package com.splice.extraction.pdf.image;

import com.splice.extraction.spi.AssetStorage;
import com.splice.model.geometry.BoundingBox;

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

            assertEquals(1, results.size());
            var bbox = results.getFirst().location().bbox();

            assertEquals(300, bbox.y(), 0.1, "Y coordinate should be converted to Top-Left origin");
        }
    }

    @Test
    @DisplayName("Should ignore small artifacts (filtering logic)")
    void shouldIgnoreTinyArtifacts() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(500, 500));
            doc.addPage(page);

            injectImageIntoPage(doc, page, createDummyImage(1, 1), 0, 0);
            injectImageIntoPage(doc, page, createDummyImage(60, 60), 100, 100);

            var results = extractor.extract(page, 1);

            assertEquals(1, results.size());

            assertEquals(60, results.getFirst().location().bbox().width(), 0.1);
        }
    }

    @Test
    @DisplayName("Should extract ONLY the image intersecting the BoundingBox")
    void shouldExtractOnlyImagesInsideRegion() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(0, 0, 500, 500));
            doc.addPage(page);

            BufferedImage img = createDummyImage(100, 100);

            injectImageIntoPage(doc, page, img, 50, 350);

            injectImageIntoPage(doc, page, img, 50, 50);

            BoundingBox topRegion = new BoundingBox(0, 0, 500, 200);

            var topResults = extractor.extractRegion(page, 1, topRegion);

            assertEquals(1, topResults.size(), "Should only find the top image");
            assertEquals(50, topResults.getFirst().location().bbox().y(), 1.0);

            BoundingBox bottomRegion = new BoundingBox(0, 300, 500, 200);

            var bottomResults = extractor.extractRegion(page, 1, bottomRegion);

            assertEquals(1, bottomResults.size(), "Should only find the bottom image");
            assertEquals(350, bottomResults.getFirst().location().bbox().y(), 1.0);
        }
    }

    @Test
    @DisplayName("Should return empty if image is outside the box")
    void shouldReturnEmptyIfNoIntersection() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(500, 500));
            doc.addPage(page);

            injectImageIntoPage(doc, page, createDummyImage(100, 100), 200, 200);

            BoundingBox cornerBox = new BoundingBox(0, 0, 50, 50);

            var results = extractor.extractRegion(page, 1, cornerBox);
            assertTrue(results.isEmpty());
        }
    }

    @Test
    @DisplayName("Should accept partial overlap")
    void shouldExtractIfImagePartiallyOverlaps() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(500, 500));
            doc.addPage(page);

            injectImageIntoPage(doc, page, createDummyImage(100, 100), 50, 350);

            BoundingBox cuttingBox = new BoundingBox(90, 50, 200, 200);

            var results = extractor.extractRegion(page, 1, cuttingBox);
            assertEquals(1, results.size(), "Should extract image even if only partially inside");
        }
    }

    @Test
    void extractRegion_nullInputs_shouldBeSafe() throws IOException {
        BoundingBox box = new BoundingBox(0,0,10,10);
        assertTrue(extractor.extractRegion(null, 1, box).isEmpty());
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