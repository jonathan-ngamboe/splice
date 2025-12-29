package com.splice.service.pdf.internal;

import com.splice.geometry.BoundingBox;

import java.util.*;
import java.util.stream.Collectors;

public class TextBlock {
    private final List<TextLine> lines = new ArrayList<>();
    private BoundingBox box;

    private float sumFontSize = 0f;
    private long charCount = 0;

    public void addLine(TextLine line) {
        if (line == null) return;

        lines.add(line);

        for (TextAtom atom : line.getAtoms()) {
            this.sumFontSize += atom.fontSize();
            this.charCount++;
        }

        if (box == null) {
            this.box = line.getBox();
        } else {
            this.box = this.box.union(line.getBox());
        }
    }

    public BoundingBox getBox() {
        return box;
    }

    public List<TextLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public int getLineCount() {
        return lines.size();
    }

    public String getText() {
        return lines.stream()
                .map(TextLine::getText)
                .collect(Collectors.joining("\n"));
    }

    public TextLine getLastLine() {
        if (lines.isEmpty()) return null;
        return lines.getLast();
    }

    public float getAverageFontSize() {
        if (charCount == 0) return 0;
        return sumFontSize / charCount;
    }

    @Override
    public String toString() {
        return "TextBlock{y=" + (box != null ? box.y() : "null") + ", lines=" + lines.size() + "}";
    }
}