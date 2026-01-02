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
    private final DetectionAlgorithm projectionDetector;

    private final CSVWriter csvWriter;

    private final TableZoneRefiner zoneRefiner;

    public TableExtractor() {
        this.latticeExtractor = new SpreadsheetExtractionAlgorithm();
        this.streamExtractor = new BasicExtractionAlgorithm();

        this.latticeDetector = new SpreadsheetDetectionAlgorithm();
        this.streamDetector = new NurminenDetectionAlgorithm();
        this.projectionDetector = new ProjectionProfileDetectionAlgorithm();

        this.csvWriter = new CSVWriter();

        this.zoneRefiner = new TableZoneRefiner();
    }

    public List<DocumentElement>  extract(Page page) {
        return extract(page, new ArrayList<>());
    }

    public List<DocumentElement> extract(Page page, List<Rectangle2D.Float> zonesToExclude) {
        if(page == null) return List.of();

        List<ExtractionTask> tasksToExecute = detect(page);

        List<Table> tables = executeTasks(page, tasksToExecute, zonesToExclude);
        System.out.println("Tables found: " + tables);
        tables.removeIf(TableExtractor::isNoise);

        return transformToDocumentElements(tables, page.getPageNumber());
    }

    private List<ExtractionTask> detect(Page page) {
        List<ExtractionTask> tasks = new ArrayList<>();

        List<Rectangle> latticeCandidates = latticeDetector.detect(page);
        List<Rectangle> streamCandidates  = streamDetector.detect(page);
        List<Rectangle> projectionCandidates = projectionDetector.detect(page);

        List<Rectangle> allCandidates = new ArrayList<>();
        allCandidates.addAll(latticeCandidates);
        allCandidates.addAll(streamCandidates);
        allCandidates.addAll(projectionCandidates);

        List<Rectangle> refinedZones = zoneRefiner.refine(allCandidates);
        System.out.println("Zones found: " + refinedZones);

        refinedZones.forEach(z ->
                tasks.add(new ExtractionTask(z, selectExtractionMethod(z, latticeCandidates))
                ));

        return tasks;
    }

    private ExtractionMethod selectExtractionMethod(Rectangle refinedZone, List<Rectangle> originalLatticeCandidates) {
        boolean touchesLattice = originalLatticeCandidates.stream()
                .anyMatch(refinedZone::intersects);

        return touchesLattice ? ExtractionMethod.LATTICE : ExtractionMethod.STREAM;
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
        System.out.println("Checking noise for table: " + table);
        if (table == null || table.getRowCount() <= 1 || table.getColCount() <= 1) {
            System.out.println("Noise detected for table: " + table);
            System.out.println("Rows: " + table.getRowCount());
            System.out.println("Cols: " + table.getColCount());
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