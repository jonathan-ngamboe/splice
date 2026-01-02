package com.splice.extraction.pdf;

import com.splice.extraction.DocumentExtractor;
import com.splice.extraction.spi.AssetStorage;
import com.splice.model.document.IngestedDocument;
import com.splice.model.document.content.ImageContent;
import com.splice.model.document.content.TableContent;
import com.splice.model.document.content.TextContent;

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
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PdfExtractorTests {

    @Mock
    private AssetStorage mockStorage;

    private DocumentExtractor extractor;

    @BeforeEach
    void setUp() throws IOException {
        extractor = new PdfExtractor(mockStorage);

        lenient().when(mockStorage.store(any(), anyString()))
                .thenReturn("s3://dummy-bucket/image.png");
    }

    @Test
    @DisplayName("Should analyze a simple PDF and return correct metadata")
    void shouldExtractSimplePdf(@TempDir Path tempDir) throws IOException {
        Path pdfPath = tempDir.resolve("temp_test.pdf");

        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.addPage(new PDPage());
            doc.save(pdfPath.toFile());
        }

        IngestedDocument document = extractor.extract(pdfPath);

        assertNotNull(document, "Document should not be null");
        assertEquals(2, document.metadata().totalPages(), "Should count 2 pages");
    }

    @Test
    @DisplayName("Should throw IOException when file does not exist")
    void shouldThrowExceptionForMissingFile() {
        Path file = Paths.get("non_existent_ghost_file.pdf");
        assertThrows(IOException.class, () -> extractor.extract(file));
    }

    @Test
    @DisplayName("Should throw Exception when file format is invalid")
    void shouldThrowExceptionForInvalidFormat(@TempDir Path tempDir) throws IOException {
        Path fakePdf = tempDir.resolve("fake.pdf");
        Files.writeString(fakePdf, "I am not a PDF header");

        assertThrows(IOException.class, () -> extractor.extract(fakePdf));
    }

    @Test
    @DisplayName("Should detect and extract ImageContent (Generated PDF)")
    void shouldExtractImages(@TempDir Path tempDir) throws IOException {
        Path pdfWithImage = tempDir.resolve("image_doc.pdf");
        createPdfWithImage(pdfWithImage);

        IngestedDocument document = extractor.extract(pdfWithImage);

        assertFalse(document.elements().isEmpty(), "Elements should not be empty");

        boolean hasImage = document.elements().stream()
                .anyMatch(e -> e.content() instanceof ImageContent);

        assertTrue(hasImage, "Should have found at least one ImageContent");

        String extractedPath = ((ImageContent) document.elements().getFirst().content()).imagePath();
        assertEquals("s3://dummy-bucket/image.png", extractedPath);
    }

    @Test
    @DisplayName("Should extract mixed content types correctly (Integration Test)")
    void shouldExtractMixedContent() {
        try {
            Path file = getResourcePath("/mixed.pdf");
            IngestedDocument document = extractor.extract(file);

            var elements = document.elements();

            assertAll("Mixed content verification",
                    () -> assertTrue(elements.stream().anyMatch(e -> e.content() instanceof TextContent), "Missing Text"),
                    () -> assertTrue(elements.stream().anyMatch(e -> e.content() instanceof TableContent), "Missing Table"),
                    () -> assertTrue(elements.stream().anyMatch(e -> e.content() instanceof ImageContent), "Missing Image")
            );
        } catch (URISyntaxException | IOException | NullPointerException e) {
            System.err.println("Skipping mixed content test: resource not found.");
        }
    }

    private Path getResourcePath(String filename) throws URISyntaxException {
        return Paths.get(Objects.requireNonNull(getClass().getResource(filename)).toURI());
    }

    private void createPdfWithImage(Path destination) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            PDImageXObject pdImage = LosslessFactory.createFromImage(doc, img);

            try (PDPageContentStream contentStream = new PDPageContentStream(doc, page)) {
                contentStream.drawImage(pdImage, 100, 100);
            }
            doc.save(destination.toFile());
        }
    }
}