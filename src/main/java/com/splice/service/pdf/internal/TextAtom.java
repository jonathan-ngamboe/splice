package com.splice.service.pdf.internal;

import com.splice.geometry.BoundingBox;

public record TextAtom (String content, float fontSize,
                        String fontName, BoundingBox box) {

    private static final float SPACE_WIDTH_FACTOR = 0.33f;
    private static final float VERTICAL_ALIGNMENT_THRESHOLD = 0.50f;

    public float getEstimatedSpaceWidth() {
        return fontSize * SPACE_WIDTH_FACTOR;
    }

    public boolean isVerticallyAlignedWith(TextAtom other) {
        return this.box.verticalOverlapRatio(other.box) > VERTICAL_ALIGNMENT_THRESHOLD;
    }
}
