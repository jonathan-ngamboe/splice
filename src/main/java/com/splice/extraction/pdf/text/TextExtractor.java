package com.splice.extraction.pdf.text;

import com.splice.extraction.pdf.text.internal.TextAtom;
import com.splice.extraction.pdf.text.internal.TextBlock;
import com.splice.extraction.pdf.text.internal.TextLine;

import com.splice.model.geometry.BoundingBox;
import com.splice.model.document.DocumentElement;
import com.splice.model.document.ElementType;
import com.splice.model.document.Location;
import com.splice.model.document.content.TextContent;

import com.splice.model.layout.LayoutElement;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.*;

import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.*;

public class TextExtractor extends PDFTextStripperByArea {
    private static final float MAX_CHAR_DISTANCE_FACTOR = 3.0f;
    private static final float MAX_LINE_SPACING_FACTOR = 1.5f;
    private static final float TITLE_SIZE_FACTOR = 1.3f;

    private final List<TextAtom> pageAtoms = new ArrayList<>();

    private BoundingBox regionOfInterest;
    private List<Rectangle2D.Float> currentZonesToExclude;

    public TextExtractor() throws IOException {
        super();
        this.setSortByPosition(false);
    }


    public List<DocumentElement> extract(PDDocument doc, int pageNumber, LayoutElement layoutElement) {
        return extract(doc, pageNumber, layoutElement, null);
    }

    public List<DocumentElement> extract(PDDocument doc, int pageNumber,
                                         LayoutElement layoutElement, List<Rectangle2D.Float> zonesToExclude) {
        if (pageNumber <= 0 || pageNumber > doc.getNumberOfPages()) {
            throw new IllegalArgumentException("Page number must be 1-based. Received: " + pageNumber);
        }

        PDPage page = doc.getPage(pageNumber - 1);
        PDRectangle mediaBox = page.getMediaBox();

        BoundingBox fullPageBox = new BoundingBox(0, 0, mediaBox.getWidth(), mediaBox.getHeight());

        return extractRegion(doc, pageNumber, fullPageBox, layoutElement, zonesToExclude);
    }

    public List<DocumentElement> extractRegion(PDDocument doc, int pageNumber, BoundingBox region, LayoutElement layoutElement) {
        return extractRegion(doc, pageNumber, region, layoutElement, null);
    }

    public List<DocumentElement> extractRegion(PDDocument doc, int pageNumber, BoundingBox region, LayoutElement layoutElement, List<Rectangle2D.Float> zonesToExclude) {
        if(doc == null) return new ArrayList<>();

        this.pageAtoms.clear();
        this.regionOfInterest = region;
        this.currentZonesToExclude = zonesToExclude != null ? zonesToExclude : new ArrayList<>();

        this.setStartPage(pageNumber);
        this.setEndPage(pageNumber);

        try {
            this.writeText(doc, Writer.nullWriter());
        } catch (IOException e) {
            throw new RuntimeException("Error while extracting region on page " + pageNumber, e);
        }

        pageAtoms.sort(TextAtom.READING_ORDER);

        List<TextLine> lines = formLines(pageAtoms);
        List<TextBlock> blocks = formBlocks(lines);

        blocks.sort(TextBlock.READING_ORDER);

        ElementType typeHint = (layoutElement != null) ? layoutElement.type() : null;

        return transformToDocumentElements(blocks, pageNumber, typeHint);
    }

    @Override
    protected void processTextPosition(TextPosition text) {
        if (text.getUnicode() == null || text.getUnicode().isBlank()) return;

        Rectangle2D.Float atomBox = new Rectangle2D.Float(
                text.getX(), text.getYDirAdj(), text.getWidth(), text.getHeight()
        );

        if (this.regionOfInterest != null) {
            Rectangle2D.Float regionRect = new Rectangle2D.Float(
                    regionOfInterest.x(), regionOfInterest.y(),
                    regionOfInterest.width(), regionOfInterest.height()
            );

            if (!regionRect.intersects(atomBox)) {
                return;
            }
        }

        boolean isInsideExcludedZone = currentZonesToExclude.stream()
                .anyMatch(zone -> zone.intersects(atomBox));

        if (isInsideExcludedZone) {
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

    private List<DocumentElement> transformToDocumentElements(List<TextBlock> blocks, int pageNumber, ElementType elementType) {
        return blocks
                .stream()
                .map(
            block -> new DocumentElement(
                    UUID.randomUUID().toString(),
                    elementType,
                    new Location(pageNumber, block.getBox()),
                    null, // Context will be extracted by parent
                    new TextContent(block.getText())
                ))
                .toList();
    }
}