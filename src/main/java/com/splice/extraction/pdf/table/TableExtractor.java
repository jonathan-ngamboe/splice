package com.splice.extraction.pdf.table;

import com.splice.model.document.content.TableContent;
import com.splice.model.geometry.BoundingBox;
import com.splice.model.document.*;

import technology.tabula.*;
import technology.tabula.detectors.*;
import technology.tabula.extractors.*;
import technology.tabula.writers.CSVWriter;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.*;

public class TableExtractor {
    private static final double LATTICE_COVERAGE_THRESHOLD = 0.50;

    private final SpreadsheetExtractionAlgorithm latticeExtractor;
    private final BasicExtractionAlgorithm streamExtractor;

    private final DetectionAlgorithm latticeDetector;
    private final DetectionAlgorithm streamDetector;

    private final CSVWriter csvWriter;

    private final TableZoneRefiner zoneRefiner;

    public TableExtractor() {
        this.latticeExtractor = new SpreadsheetExtractionAlgorithm();
        this.streamExtractor = new BasicExtractionAlgorithm();

        this.latticeDetector = new SpreadsheetDetectionAlgorithm();
        this.streamDetector = new NurminenDetectionAlgorithm();

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
        tables.removeIf(TableValidator::isNoise);

        return transformToDocumentElements(tables, page.getPageNumber());
    }

    private List<ExtractionTask> detect(Page page) {
        List<ExtractionTask> tasks = new ArrayList<>();


        List<Rectangle> latticeCandidates = latticeDetector.detect(page);
        List<Rectangle> streamCandidates  = streamDetector.detect(page);

        List<Rectangle> allCandidates = new ArrayList<>();
        allCandidates.addAll(latticeCandidates);
        allCandidates.addAll(streamCandidates);

        List<Rectangle> refinedZones = zoneRefiner.refine(allCandidates);

        refinedZones.forEach(z ->
                tasks.add(new ExtractionTask(z, selectExtractionMethod(z, latticeCandidates))
                ));

        return tasks;
    }

    private ExtractionMethod selectExtractionMethod(Rectangle refinedZone, List<Rectangle> originalLatticeCandidates) {
        float totalArea = refinedZone.getArea();
        if (totalArea == 0) return ExtractionMethod.STREAM;

        double latticeCoveredArea = 0;

        for (Rectangle latticeRect : originalLatticeCandidates) {
            float interLeft = Math.max(refinedZone.getLeft(), latticeRect.getLeft());
            float interTop = Math.max(refinedZone.getTop(), latticeRect.getTop());
            float interRight = Math.min(refinedZone.getRight(), latticeRect.getRight());
            float interBottom = Math.min(refinedZone.getBottom(), latticeRect.getBottom());

            float interWidth = Math.max(0, interRight - interLeft);
            float interHeight = Math.max(0, interBottom - interTop);

            latticeCoveredArea += (interWidth * interHeight);
        }

        double coverageRatio = latticeCoveredArea / totalArea;

        return coverageRatio > LATTICE_COVERAGE_THRESHOLD ? ExtractionMethod.LATTICE : ExtractionMethod.STREAM;
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