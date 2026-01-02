package com.splice.extraction.pdf.table;

import com.splice.model.geometry.BoundingBox;
import technology.tabula.Rectangle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TableZoneRefiner {
    public List<Rectangle> refine(List<Rectangle> rawRectangles) {
        if (rawRectangles == null || rawRectangles.isEmpty()) {
            return new ArrayList<>();
        }

        List<Rectangle> sortedRectangles = new ArrayList<>(rawRectangles);

        sortedRectangles.sort(Comparator.comparingDouble(Rectangle::getTop)
                .thenComparingDouble(Rectangle::getLeft));

        return mergeOverlappingRectangles(sortedRectangles);
    }

    private List<Rectangle> mergeOverlappingRectangles(List<Rectangle> sortedRectangles) {
        List<Rectangle> mergedRectangles = new ArrayList<>();

        BoundingBox currentBuffer = toBoundingBox(sortedRectangles.getFirst());

        for (int i = 1; i < sortedRectangles.size(); i++) {
            BoundingBox next = toBoundingBox(sortedRectangles.get(i));

            if (currentBuffer.intersects(next)) {
                currentBuffer = currentBuffer.union(next);
            } else {
                mergedRectangles.add(toTabulaRectangle(currentBuffer));
                currentBuffer = next;
            }
        }

        mergedRectangles.add(toTabulaRectangle(currentBuffer));
        return mergedRectangles;
    }

    private BoundingBox toBoundingBox(Rectangle r) {
        return new BoundingBox(r.getLeft(), r.getTop(), (float) r.getWidth(), (float) r.getHeight());
    }

    private Rectangle toTabulaRectangle(BoundingBox b) {
        return new Rectangle(b.y(), b.x(), b.width(), b.height());
    }
}