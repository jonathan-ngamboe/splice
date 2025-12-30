package com.splice.extraction.pdf.text.internal;

import com.splice.model.geometry.BoundingBox;

import java.util.Comparator;

public record TextAtom (String content, float fontSize,
                        String fontName, BoundingBox box) {

    private static final float SPACE_WIDTH_FACTOR = 0.33f;
    private static final float VERTICAL_ALIGNMENT_THRESHOLD = 0.50f;
    private static final float Y_TOLERANCE = 1.0f;

    public float getEstimatedSpaceWidth() {
        return fontSize * SPACE_WIDTH_FACTOR;
    }

    public boolean isVerticallyAlignedWith(TextAtom other) {
        return this.box.verticalOverlapRatio(other.box) > VERTICAL_ALIGNMENT_THRESHOLD;
    }

    public static final Comparator<TextAtom> READING_ORDER = (a, b) -> {
        boolean onSameLine = Math.abs(a.box().y() - b.box().y()) <= Y_TOLERANCE;

        if (onSameLine) {
            return Float.compare(a.box().x(), b.box().x());
        }
        return Float.compare(a.box().y(), b.box().y());
    };
}
