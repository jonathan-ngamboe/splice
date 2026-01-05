package com.splice.extraction.pdf.text;

import com.splice.model.document.ElementType;
import com.splice.model.document.content.TextContent;
import com.splice.model.geometry.BoundingBox;
import com.splice.model.layout.LayoutElement;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class TextExtractorTests {
    private final TextExtractor extractor;

    TextExtractorTests() throws IOException {
        this.extractor = new TextExtractor();
    }

    @Test
    @DisplayName("Should extract simple text content from a generated PDF page")
    void shouldExtractTextFromSimplePage() throws IOException {
        try (PDDocument document = createInMemoryDocument("Hello World", 50, 700)) {
            var results = extractor.extract(document, 1, null);

            assertAll("Basic extraction check",
                    () -> assertFalse(results.isEmpty(), "Should return at least one element"),
                    () -> assertEquals(1, results.size(), "Should return exactly one block of text"),
                    () -> assertTrue(((TextContent) results.getFirst().content()).text().contains("Hello World"))
            );
        }
    }

    @Test
    @DisplayName("Should return empty list for a completely blank page")
    void shouldReturnEmptyForBlankPage() throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());

            var results = extractor.extract(document, 1, null);

            boolean contentIsEmpty = results.isEmpty() ||
                    results.stream().allMatch(el -> ((TextContent)el.content()).text().isBlank());

            assertTrue(contentIsEmpty, "Should return no meaningful content for blank page");
        }
    }

    @Test
    @DisplayName("Should throw exception or return empty if page index is invalid")
    void shouldFailGracefullyOnInvalidPage() throws IOException {
        try (PDDocument document = createInMemoryDocument("Test", 50, 50)) {
            assertThrows(RuntimeException.class, () -> extractor.extract(document, 99, null));
        }
    }

    @Test
    @DisplayName("Should extract ONLY text within the specified bounding box (Inclusion)")
    void shouldExtractTextFromSpecificRegion() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            writeText(document, page, "TARGET_DATA", 50, 750);
            writeText(document, page, "NOISE_DATA", 50, 50);

            BoundingBox topRegion = new BoundingBox(0, 0, 500, 300);

            var results = extractor.extractRegion(document, 1, topRegion, null);

            assertFalse(results.isEmpty(), "Should find text in the top region");

            String extractedText = ((TextContent) results.getFirst().content()).text();

            assertAll("Region inclusion check",
                    () -> assertTrue(extractedText.contains("TARGET_DATA"), "Should contain the text inside the box"),
                    () -> assertFalse(extractedText.contains("NOISE_DATA"), "Should NOT contain text outside the box")
            );
        }
    }

    @Test
    @DisplayName("Should return empty list when region contains no text")
    void shouldReturnEmptyWhenRegionIsEmpty() throws IOException {
        try (PDDocument document = createInMemoryDocument("Content is here", 100, 100)) {
            BoundingBox emptyTopCorner = new BoundingBox(0, 0, 100, 100);

            var results = extractor.extractRegion(document, 1, emptyTopCorner, null);

            assertTrue(results.isEmpty(), "Should extract nothing from an empty area");
        }
    }

    @Test
    @DisplayName("Should handle null BoundingBox or Document gracefully")
    void shouldHandleNullArgumentsInRegion() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            var res1 = extractor.extractRegion(doc, 1, null, null);
            assertTrue(res1.isEmpty(), "Null box should result in empty list");

            var res2 = extractor.extractRegion(null, 1, new BoundingBox(0,0,10,10), null);
            assertTrue(res2.isEmpty(), "Null doc should result in empty list");
        }
    }

    @Test
    @DisplayName("Should force ElementType if LayoutElement provides a hint (e.g. TITLE)")
    void shouldUseTypeHintFromLayoutElement() throws IOException {
        try (PDDocument document = createInMemoryDocument("This is a Title", 50, 700)) {

            LayoutElement yoloHint = new LayoutElement(
                    0.95,
                    ElementType.TITLE,
                    new BoundingBox(0, 0, 500, 200)
            );

            var results = extractor.extract(document, 1, yoloHint);

            assertFalse(results.isEmpty());

            assertEquals(ElementType.TITLE, results.getFirst().type(),
                    "Extractor should respect the type hint provided by layout element");
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