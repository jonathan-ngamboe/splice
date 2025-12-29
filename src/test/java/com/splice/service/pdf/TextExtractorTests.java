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
    private final TextExtractor extractor;

    TextExtractorTests() {
        this.extractor = new TextExtractor();
    }

    @Test
    @DisplayName("Should extract simple text content from a generated PDF page")
    void shouldExtractTextFromSimplePage() throws IOException {
        try (PDDocument document = createInMemoryDocument("Hello World", 50, 700)) {
            var results = extractor.extract(document, 1);

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

            writeText(document, page, "Secret Data", 50, 750);

            writeText(document, page, "Public Info", 50, 100);

            List<Rectangle2D.Float> exclusions = List.of(new Rectangle2D.Float(0, 0, 500, 200));

            var results = extractor.extract(document, 1, exclusions);

            String fullExtractedText = results.stream()
                    .map(el -> ((TextContent) el.content()).text())
                    .reduce("", (a, b) -> a + " " + b);

            assertAll("Exclusion zone check",
                    () -> assertTrue(fullExtractedText.contains("Public Info"), "Should keep the text outside the zone"),
                    () -> assertFalse(fullExtractedText.contains("Secret Data"), "Should have removed the text inside the zone")
            );
        }
    }

    @Test
    @DisplayName("Should handle null exclusion list safely (treat as empty)")
    void shouldExtractEverythingWhenExclusionsAreNull() throws IOException {
        try (PDDocument document = createInMemoryDocument("Test Content", 50, 700)) {
            var results = extractor.extract(document, 1);

            assertFalse(results.isEmpty());
            assertTrue(((TextContent) results.getFirst().content()).text().contains("Test Content"));
        }
    }

    @Test
    @DisplayName("Should return empty list for a completely blank page")
    void shouldReturnEmptyForBlankPage() throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());

            var results = extractor.extract(document, 1);

            boolean contentIsEmpty = results.isEmpty() ||
                    results.stream().allMatch(el -> ((TextContent)el.content()).text().isBlank());

            assertTrue(contentIsEmpty, "Should return no meaningful content for blank page");
        }
    }

    @Test
    @DisplayName("Should throw exception or return empty if page index is invalid")
    void shouldFailGracefullyOnInvalidPage() throws IOException {
        try (PDDocument document = createInMemoryDocument("Test", 50, 50)) {
            assertThrows(RuntimeException.class, () -> extractor.extract(document, 99));
        }
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