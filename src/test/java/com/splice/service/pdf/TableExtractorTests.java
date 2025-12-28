package com.splice.service.pdf;

import static org.junit.jupiter.api.Assertions.*;

import com.splice.model.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.*;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class TableExtractorTests {

    private static final TableExtractor extractor = new TableExtractor();

    private static PDDocument document;
    private static List<DocumentElement> results;

    @BeforeAll
    static void setupAll() throws IOException, URISyntaxException {
        File pdfFile = Paths.get(Objects.requireNonNull(TableExtractorTests.class.getResource("/table.pdf")).toURI()).toFile();

        document = Loader.loadPDF(pdfFile);

        try (ObjectExtractor tabulaExtractor = new ObjectExtractor(document)) {
            Page realTabulaPage = tabulaExtractor.extract().next();
            results = extractor.extract(realTabulaPage);
        }
    }

    @AfterAll
    static void tearDownAll() throws IOException {
        if (document != null) {
            document.close();
        }
    }

    @Test
    @DisplayName("Should detect exactly one table in the sample file")
    void shouldDetectTable() {
        assertNotNull(results);
        assertFalse(results.isEmpty(), "List should not be empty");
        assertEquals(1, results.size(), "Should find exactly 1 table");
        assertInstanceOf(TableContent.class, results.getFirst().content());
    }

    @Test
    @DisplayName("Should extract the correct CSV content containing 'Price'")
    void shouldExtractCsvContent() {
        DocumentElement element = results.getFirst();
        TableContent content = (TableContent) element.content();

        assertNotNull(content.csvData());
        assertTrue(content.csvData().contains("Price"), "CSV should contain the header 'Price'");
    }

    @Test
    @DisplayName("Should map geometry correctly (Width and Height > 0)")
    void shouldMapGeometry() {
        DocumentElement element = results.getFirst();
        Location loc = element.location();

        assertNotNull(loc.bbox());
        assertTrue(loc.bbox().width() > 0, "Width should be valid");
        assertTrue(loc.bbox().height() > 0, "Height should be valid");
    }

    @Test
    void extract_nullPage_shouldReturnEmptyList() {
        assertTrue(extractor.extract(null).isEmpty());
    }
}