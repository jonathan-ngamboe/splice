package com.splice.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.splice.model.ImageContent;
import com.splice.model.TextContent;
import com.splice.service.DocumentAnalyzer;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

class PdfAnalyzerTests {
    private final DocumentAnalyzer analyzer = new PdfAnalyzer();

    @Test
    public void analyze_validPdf_shouldReturnNonEmptyList() throws IOException, URISyntaxException {
        Path file = Paths.get(Objects.requireNonNull(getClass().getResource("/sample.pdf")).toURI());
        var result = analyzer.analyze(file);
        assertFalse(result.isEmpty(), "List should not be empty for a valid Pdf.");
    }

    @Test
    public void analyze_multiPagePdf_shouldReturnCorrectPageCount() throws IOException, URISyntaxException  {
        Path file = Paths.get(Objects.requireNonNull(getClass().getResource("/three_pages.pdf")).toURI());
        var result = analyzer.analyze(file);
        assertEquals(3, result.size());
    }

    @Test
    public void analyze_nonExistentFile_shouldThrowIOException() {
        Path file = Paths.get("non_existant_file.pdf");
        assertThrows(IOException.class, () -> {
            analyzer.analyze(file);
        });
    }

    @Test
    public void analyze_invalidFormat_shouldThrowException() throws URISyntaxException {
        Path file = Paths.get(Objects.requireNonNull(getClass().getResource("/fake.txt")).toURI());
        assertThrows(IOException.class, () -> {
            analyzer.analyze(file);
        });
    }

    @Test
    public void analyze_imagePdf_shouldReturnImageContent() throws IOException, URISyntaxException {
        Path file = Paths.get(Objects.requireNonNull(getClass().getResource("/scanned.pdf")).toURI());
        var result = analyzer.analyze(file);
        assertInstanceOf(ImageContent.class, result.getFirst());
    }

    @Test
    public void analyze_mixedPdf_shouldReturnBothContentTypes() throws IOException, URISyntaxException {
        Path file = Paths.get(Objects.requireNonNull(getClass().getResource("/mixed.pdf")).toURI());
        var result = analyzer.analyze(file);
        assertAll(
                () -> assertEquals(2,
                        result.size(),
                        "List should contain 2 elements."),
                () -> assertEquals(1,
                        result.stream().filter(
                                c -> c instanceof TextContent).count(),
                        "There must be a TextContent."),
                () -> assertEquals(1,
                        result.stream().filter(
                                c -> c instanceof ImageContent).count(),
                        "There must be an ImageContent.")
        );
    }
}
