package com.splice.extraction.pdf.text;

import com.splice.extraction.pdf.text.internal.TextAtom;
import com.splice.extraction.pdf.text.internal.TextBlock;
import com.splice.extraction.pdf.text.internal.TextLine;
import com.splice.model.geometry.BoundingBox;
import com.splice.model.document.DocumentElement;
import com.splice.model.document.ElementType;
import com.splice.model.document.Location;
import com.splice.model.document.content.TextContent;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.*;

import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.*;

public class TextExtractor extends PDFTextStripper {
    private static final float MAX_CHAR_DISTANCE_FACTOR = 3.0f;
    private static final float MAX_LINE_SPACING_FACTOR = 1.5f;
    private static final float TITLE_SIZE_FACTOR = 1.3f;

    private final List<TextAtom> pageAtoms = new ArrayList<>();
    private List<Rectangle2D.Float> currentZonesToExclude;

    public TextExtractor() {
        super();
        this.setSortByPosition(false);
    }

    public List<DocumentElement> extract(PDDocument doc, int pageNumber) {
        return extract(doc, pageNumber, null);
    }

    public List<DocumentElement> extract(PDDocument doc, int pageNumber, List<Rectangle2D.Float> zonesToExclude) {        this.pageAtoms.clear();
        if (pageNumber <= 0 || pageNumber > doc.getNumberOfPages()) {
            throw new IllegalArgumentException("Page number must be 1-based (PDF standard). Received: " + pageNumber);
        }

        this.pageAtoms.clear();
        this.currentZonesToExclude = zonesToExclude != null ? zonesToExclude : new ArrayList<>();

        this.setStartPage(pageNumber);
        this.setEndPage(pageNumber);

        Writer dummyWriter = new OutputStreamWriter(new ByteArrayOutputStream());
        try {
            this.writeText(doc, dummyWriter);
        } catch (IOException e) {
            throw new RuntimeException("Error while extracting the page " + pageNumber, e);
        }

        pageAtoms.sort(TextAtom.READING_ORDER);

        List<TextLine> lines = formLines(pageAtoms);
        List<TextBlock> blocks = formBlocks(lines);

        blocks.sort(TextBlock.READING_ORDER);

        return transformToDocumentElements(blocks, pageNumber);
    }

    @Override
    protected void processTextPosition(TextPosition text) {
        if (text.getUnicode() == null || text.getUnicode().isBlank()) return;

        Rectangle2D.Float atomBox = new Rectangle2D.Float(
                text.getX(), text.getYDirAdj(), text.getWidth(), text.getHeight()
        );

        boolean isInsideTable = currentZonesToExclude.stream()
                .anyMatch(zone -> zone.intersects(atomBox));

        if (isInsideTable) {
            return;
        }

        String normalized = java.text.Normalizer.normalize(text.getUnicode(), java.text.Normalizer.Form.NFKC);

        pageAtoms.add(
            new TextAtom(
                normalized,
                text.getFontSizeInPt(),
                text.getFont().getName(),
                new BoundingBox(atomBox.x, atomBox.y, atomBox.width, atomBox.height)
            )
        );
    }

    private List<TextLine> formLines(List<TextAtom> atoms) {
        List<TextLine> lines = new ArrayList<>();
        TextLine currentLine = null;

        for(var atom : atoms) {
            if(currentLine == null) {
                currentLine = new TextLine();
                currentLine.addAtom(atom);
            } else {
                if (currentLine.accepts(atom, MAX_CHAR_DISTANCE_FACTOR)) {
                    currentLine.addAtom(atom);
                } else {
                    lines.add(currentLine);
                    currentLine = new TextLine();
                    currentLine.addAtom(atom);
                }
            }
        }

        if(currentLine != null) lines.add(currentLine);

        return lines;
    }

    private List<TextBlock> formBlocks(List<TextLine> lines) {
        List<TextBlock> blocks = new ArrayList<>();
        TextBlock currentBlock = null;

        for(var line : lines) {
            if(currentBlock == null) {
                currentBlock = new TextBlock();
                currentBlock.addLine(line);
            } else {
                if(currentBlock.accepts(line, MAX_LINE_SPACING_FACTOR)) {
                    currentBlock.addLine(line);
                } else {
                    blocks.add(currentBlock);
                    currentBlock = new TextBlock();
                    currentBlock.addLine(line);
                }
            }
        }

        if(currentBlock != null) blocks.add(currentBlock);

        return blocks;
    }

    private float calculateBodyFontSize(List<TextAtom> atoms) {
        if (atoms.isEmpty()) return 12.0f;

        Map<Float, Integer> frequencyMap = new HashMap<>();

        for (TextAtom atom : atoms) {
            float roundedSize = Math.round(atom.fontSize() * 2) / 2.0f;
            frequencyMap.put(roundedSize, frequencyMap.getOrDefault(roundedSize, 0) + 1);
        }

        return frequencyMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(12.0f);
    }

    private ElementType determineType(TextBlock block, float bodySize) {
        float averageFontSize = block.getAverageFontSize();
        if (averageFontSize > bodySize * TITLE_SIZE_FACTOR) {
            return ElementType.TITLE;
        }
        return ElementType.NARRATIVE_TEXT;
    }

    private List<DocumentElement> transformToDocumentElements(List<TextBlock> blocks, int pageNumber) {
        float bodyFontSize = calculateBodyFontSize(pageAtoms);

        return blocks
                .stream()
                .map(
            block -> new DocumentElement(
                    UUID.randomUUID().toString(),
                    determineType(block, bodyFontSize),
                    new Location(pageNumber, block.getBox()),
                    null, // Context will be extracted by parent
                    new TextContent(block.getText())
                ))
                .toList();
    }
}