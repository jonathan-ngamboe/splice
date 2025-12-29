package com.splice.service.pdf.internal;

import com.splice.geometry.BoundingBox;
import java.util.Comparator;

public class XYBlockComparator implements Comparator<TextBlock> {
    private static final float Y_OVERLAP_THRESHOLD = 0.40f;

    @Override
    public int compare(TextBlock o1, TextBlock o2) {
        BoundingBox b1 = o1.getBox();
        BoundingBox b2 = o2.getBox();

        float overlap = b1.verticalOverlapRatio(b2);

        boolean onSameLine = overlap > Y_OVERLAP_THRESHOLD;

        if (onSameLine) {
            return Float.compare(b1.x(), b2.x());
        }

        return Float.compare(b1.y(), b2.y());
    }
}