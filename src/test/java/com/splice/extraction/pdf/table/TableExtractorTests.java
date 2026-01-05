package com.splice.extraction.pdf.table;

import com.splice.model.document.DocumentElement;
import com.splice.model.document.Location;
import com.splice.model.document.content.TableContent;
import com.splice.model.geometry.BoundingBox;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

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
    private static Page tabulaPage;
    private static List<DocumentElement> results;

    @BeforeAll
    static void setupAll() throws IOException, URISyntaxException {
        File pdfFile = Paths.get(Objects.requireNonNull(TableExtractorTests.class.getResource("/table.pdf")).toURI()).toFile();

        document = Loader.loadPDF(pdfFile);

        try (ObjectExtractor tabulaExtractor = new ObjectExtractor(document)) {
            tabulaPage = tabulaExtractor.extract().next();
            results = extractor.extract(tabulaPage);
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

    @Test
    @DisplayName("SShould extract table when given the correct BoundingBox")
    void extractRegionWithValidBox() {
        var correctBox = results.getFirst().location().bbox();

        List<DocumentElement> regionResults = extractor.extractRegion(tabulaPage, correctBox);

        assertNotNull(regionResults);
        assertEquals(1, regionResults.size(), "Should find 1 table in the defined region");

        TableContent content = (TableContent) regionResults.getFirst().content();
        assertTrue(content.csvData().contains("Price"), "Region extracted content should match expectation");
    }

    @Test
    @DisplayName("Should return empty list when looking at an empty margin")
    void extractRegionWithEmptyZone() {
        var emptyMarginBox = new BoundingBox(0f, 0f, 10f, 10f);

        List<DocumentElement> regionResults = extractor.extractRegion(tabulaPage, emptyMarginBox);

        assertNotNull(regionResults);
        assertTrue(regionResults.isEmpty(), "Should not find any table in the top-left margin");
    }

    @Test
    @DisplayName("Should be robust to null arguments")
    void extractRegionNullArgs() {
        var box = new BoundingBox(10f, 10f, 100f, 100f);

        assertTrue(extractor.extractRegion(null, box).isEmpty(), "Null page should return empty");
        assertTrue(extractor.extractRegion(tabulaPage, null).isEmpty(), "Null box should return empty");
    }
}