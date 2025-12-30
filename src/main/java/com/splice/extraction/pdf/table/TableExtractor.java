package com.splice.extraction.pdf.table;

import com.splice.model.geometry.BoundingBox;

import com.splice.model.document.DocumentElement;
import com.splice.model.document.ElementType;
import com.splice.model.document.Location;
import com.splice.model.document.content.TableContent;
import technology.tabula.*;
import technology.tabula.detectors.*;
import technology.tabula.extractors.*;
import technology.tabula.writers.CSVWriter;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.*;

public class TableExtractor {
    private final SpreadsheetExtractionAlgorithm latticeExtractor;
    private final BasicExtractionAlgorithm streamExtractor;

    private final DetectionAlgorithm latticeDetector;
    private final DetectionAlgorithm streamDetector;

    private final CSVWriter csvWriter;

    public TableExtractor() {
        this.latticeExtractor = new SpreadsheetExtractionAlgorithm();
        this.streamExtractor = new BasicExtractionAlgorithm();

        this.latticeDetector = new SpreadsheetDetectionAlgorithm();
        this.streamDetector = new NurminenDetectionAlgorithm();

        this.csvWriter = new CSVWriter();
    }

    public List<DocumentElement>  extract(Page page) {
        return extract(page, new ArrayList<>());
    }

    public List<DocumentElement> extract(Page page, List<Rectangle2D.Float> zonesToExclude) {
        if(page == null) return List.of();

        List<ExtractionTask> tasksToExecute = detectAndResolveConflicts(page);

        List<Table> tables = executeTasks(page, tasksToExecute, zonesToExclude);
        tables.removeIf(TableExtractor::isNoise);

        return transformToDocumentElements(tables, page.getPageNumber());
    }

    private List<ExtractionTask> detectAndResolveConflicts(Page page) {
        List<ExtractionTask> tasks = new ArrayList<>();

        var latticeCandidates = latticeDetector.detect(page);
        latticeCandidates.forEach(c -> tasks.add(new ExtractionTask(c, ExtractionMethod.LATTICE)));

        var streamCandidates  = new ArrayList<>(streamDetector.detect(page));
        streamCandidates.removeIf(c -> isOverlappingWithAny(c, latticeCandidates));
        streamCandidates.forEach(c -> tasks.add(new ExtractionTask(c, ExtractionMethod.STREAM)));

        return tasks;
    }

    private List<Table> executeTasks(Page page, List<ExtractionTask> tasks, List<Rectangle2D.Float> awtZonesToExclude) {
        List<Table> tables = new ArrayList<>();

        List<technology.tabula.Rectangle> tabulaExclusions = awtZonesToExclude.stream()
                .map(this::convertAwtToTabula)
                .toList();

        for(var task : tasks) {
            var workingMethod = task.method;
            var region = task.bounds();

            boolean isExcluded = tabulaExclusions.stream().anyMatch(exclusion -> {
                float centerX = region.x + (region.width / 2);
                float centerY = region.y + (region.height / 2);
                return exclusion.contains(centerX, centerY);
            });

            if (isExcluded) {
                continue;
            }

            var workingArea = page.getArea(region);

            if (workingMethod == ExtractionMethod.LATTICE)
                tables.addAll(latticeExtractor.extract(workingArea));
            else if (workingMethod == ExtractionMethod.STREAM)
                tables.addAll(streamExtractor.extract(workingArea));
        }

        return tables;
    }

    private static boolean isNoise(Table table) {
        if (table == null || table.getRowCount() <= 1 || table.getColCount() <= 1) {
            return true;
        }

        boolean hasContent = false;

        for (var row : table.getRows()) {
            for (var cell : row) {
                if (!cell.getText().trim().isEmpty()) {
                    hasContent = true;
                    break;
                }
            }
            if (hasContent) break;
        }

        return !hasContent;
    }

    private List<DocumentElement> transformToDocumentElements(List<Table> tables, int pageNumber) {
        List<DocumentElement> documentElements = new ArrayList<>();

        for(var t : tables) {
            var tableLocation = new Location(pageNumber, new BoundingBox(
                    (float) t.getMinX(), (float) t.getMinY(), t.width, t.height
            ));

            var tableContent = new TableContent(convertTableToCsvString(t));

            var documentElement = new DocumentElement(
                    UUID.randomUUID().toString(),
                    ElementType.TABLE,
                    tableLocation,
                    null, // Context will be extracted by parent
                    tableContent
            );

            documentElements.add(documentElement);
        }

        return documentElements;
    }

    private String convertTableToCsvString(Table table) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            csvWriter.write(stringBuilder, table);
        } catch (IOException e) {
            throw new RuntimeException("Critical error while writing CSV to memory", e);
        }
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    private boolean isOverlappingWithAny(Rectangle candidate, List<Rectangle> existingAreas) {
        return existingAreas.stream().anyMatch(candidate::intersects);
    }

    private technology.tabula.Rectangle convertAwtToTabula(Rectangle2D.Float awtRect) {
        return new technology.tabula.Rectangle(
                awtRect.y,
                awtRect.x,
                awtRect.width,
                awtRect.height
        );
    }

    private enum ExtractionMethod {
        LATTICE, STREAM
    }

    private record ExtractionTask(Rectangle bounds, ExtractionMethod method) {}
}