package com.splice.service.pdf;

import com.splice.model.ImageContent;
import com.splice.model.IngestedDocument;
import com.splice.model.TableContent;
import com.splice.model.TextContent;
import com.splice.service.DocumentAnalyzer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class PdfAnalyzerTests {
    private final DocumentAnalyzer analyzer = new PdfAnalyzer();

    @Test
    @DisplayName("Should analyze a simple PDF and return correct metadata and elements")
    void shouldAnalyzeSimplePdf(@TempDir Path tempDir) throws IOException {
        Path pdfPath = tempDir.resolve("temp_test.pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.addPage(new PDPage());
            doc.save(pdfPath.toFile());
        }

        IngestedDocument document = analyzer.analyze(pdfPath);

        assertNotNull(document, "Document should not be null");
        assertEquals(2, document.metadata().totalPages(), "Should count 2 pages");
        assertEquals(2, document.elements().size());
    }

    @Test
    @DisplayName("Should throw IOException when file does not exist")
    void shouldThrowExceptionForMissingFile() {
        Path file = Paths.get("non_existent_ghost_file.pdf");
        assertThrows(IOException.class, () -> analyzer.analyze(file));
    }

    @Test
    @DisplayName("Should throw Exception when file format is invalid (text file renamed as pdf)")
    void shouldThrowExceptionForInvalidFormat(@TempDir Path tempDir) throws IOException {
        Path fakePdf = tempDir.resolve("fake.pdf");
        Files.writeString(fakePdf, "I am not a PDF header");

        assertThrows(IOException.class, () -> analyzer.analyze(fakePdf));
    }

    @Test
    @DisplayName("Should detect and extract ImageContent from scanned PDF")
    void shouldExtractImages() throws URISyntaxException, IOException {
        Path file = getResourcePath("/scanned.pdf");

        IngestedDocument document = analyzer.analyze(file);

        assertFalse(document.elements().isEmpty());
        boolean hasImage = document.elements().stream()
                .anyMatch(e -> e.content() instanceof ImageContent);

        assertTrue(hasImage, "Should have found at least one image content");
    }

    @Test
    @DisplayName("Should extract mixed content types correctly")
    void shouldExtractMixedContent() throws URISyntaxException, IOException {
        Path file = getResourcePath("/mixed.pdf");
        IngestedDocument document = analyzer.analyze(file);

        var elements = document.elements();

        assertAll("Mixed content verification",
                () -> assertTrue(elements.stream().anyMatch(e -> e.content() instanceof TextContent), "Missing Text"),
                () -> assertTrue(elements.stream().anyMatch(e -> e.content() instanceof TableContent), "Missing Table"),
                () -> assertTrue(elements.stream().anyMatch(e -> e.content() instanceof ImageContent), "Missing Image")
        );
    }

    private Path getResourcePath(String filename) throws URISyntaxException {
        return Paths.get(Objects.requireNonNull(getClass().getResource(filename)).toURI());
    }
}