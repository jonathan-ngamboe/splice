package com.splice.service.pdf;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import com.splice.model.*;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import technology.tabula.*;

import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.net.URISyntaxException;

public class TableExtractorTests {
    private final TableExtractor extractor = new TableExtractor();
    private final Page mockedPage = mock(technology.tabula.Page.class);

    private File getResourceFile() throws URISyntaxException {
        return Paths.get(Objects.requireNonNull(getClass().getResource("/table.pdf")).toURI()).toFile();
    }

    @Test
    public void extract_realPdfWithTable_shouldDetectTableAndContent() throws IOException, URISyntaxException {
        File pdfFile = getResourceFile();

        try (PDDocument document = Loader.loadPDF(pdfFile);
             ObjectExtractor tabulaExtractor = new ObjectExtractor(document)) {

            Page realPage = tabulaExtractor.extract().next();

            List<DocumentElement> result = extractor.extract(realPage);

            assertFalse(result.isEmpty(), "Should detect at least one table");

            DocumentElement element = result.get(0);
            assertInstanceOf(TableContent.class, element.content(), "The content must be of type TableContent.");

            TableContent tableContent = (TableContent) element.content();
            assertNotNull(tableContent.csvData(), "The CSV must not be null");
            assertTrue(tableContent.csvData().contains("Price"), "The table must contain the header ‘Price’.");
        }
    }

    @Test
    public void extract_validPage_shouldMapCoordinatesToLocation() {
        double minX = 10.0;
        double minY = 20.0;
        double maxX = 30.0;
        double maxY = 40.0;
        int pageNumber = 5;

        when(mockedPage.getMinX()).thenReturn(minX);
        when(mockedPage.getMinY()).thenReturn(minY);
        when(mockedPage.getMaxX()).thenReturn(maxX);
        when(mockedPage.getMaxY()).thenReturn(maxY);
        when(mockedPage.getPageNumber()).thenReturn(pageNumber);

        var result = extractor.extract(mockedPage);

        assertNotNull(result, "The result must never be null.");
        assertFalse(result.isEmpty(), "At least one item must have been found.");

        var location = result.getFirst().location();
        assertEquals(pageNumber, location.pageNumber());
        assertEquals((float) minX, location.bbox().getLowerLeftX(), 0.01);
    }

    @Test
    public void extract_nullPage_shouldReturnEmptyList() {
        var result = extractor.extract(null);
        assertTrue(result.isEmpty());
    }
}
