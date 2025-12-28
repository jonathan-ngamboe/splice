package com.splice.service.pdf;

import com.splice.model.TextContent;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.*;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextExtractorTests {
    private final TextExtractor extractor = new TextExtractor();

    @Test
    @DisplayName("Should extract simple text content from a generated PDF page")
    void shouldExtractTextFromSimplePage() throws IOException {
        try (PDDocument document = createInMemoryDocument("Hello World", 50, 700)) {
            PDPage page = document.getPage(0);

            var results = extractor.extract(page);

            assertAll("Basic extraction check",
                    () -> assertFalse(results.isEmpty(), "Should return at least one element"),
                    () -> assertEquals(1, results.size(), "Should return exactly one block of text"),
                    () -> assertTrue(((TextContent) results.getFirst().content()).text().contains("Hello World"))
            );
        }
    }

    @Test
    @DisplayName("Should ignore text located inside exclusion rectangles")
    void shouldExtractTextIgnoringExcludedZones() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            writeText(document, page, "Important Text", 50, 700);
            writeText(document, page, "Secret Data", 50, 100);

            List<Rectangle2D.Float> exclusions = List.of(new Rectangle2D.Float(0, 0, 200, 200));

            var results = extractor.extract(page, exclusions);

            String extractedText = ((TextContent) results.getFirst().content()).text();

            assertAll("Exclusion zone check",
                    () -> assertTrue(extractedText.contains("Important Text"), "Should keep the main text"),
                    () -> assertFalse(extractedText.contains("Secret Data"), "Should have removed the secret data")
            );
        }
    }

    @Test
    @DisplayName("Should handle null exclusion list safely (treat as empty)")
    void shouldExtractEverythingWhenExclusionsAreNull() throws IOException {
        try (PDDocument document = createInMemoryDocument("Test Content", 50, 700)) {
            PDPage page = document.getPage(0);

            var results = extractor.extract(page, null);

            assertFalse(results.isEmpty());
            assertTrue(((TextContent) results.getFirst().content()).text().contains("Test Content"));
        }
    }

    @Test
    @DisplayName("Should return empty list or empty text for a completely blank page")
    void shouldReturnEmptyForBlankPage() throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());

            var results = extractor.extract(document.getPage(0));

            assertTrue(results.isEmpty() || ((TextContent)results.getFirst().content()).text().isBlank());
        }
    }

    @Test
    @DisplayName("Should return empty list when input page is null")
    void shouldReturnEmptyListWhenPageIsNull() {
        var results = extractor.extract(null);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    private PDDocument createInMemoryDocument(String text, float x, float y) throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);
        writeText(document, page, text, x, y);
        return document;
    }

    private void writeText(PDDocument doc, PDPage page, String text, float x, float y) throws IOException {
        try (PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            contentStream.newLineAtOffset(x, y);
            contentStream.showText(text);
            contentStream.endText();
        }
    }
}