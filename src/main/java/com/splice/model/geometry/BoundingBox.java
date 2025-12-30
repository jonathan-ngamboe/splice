package com.splice.model.geometry;

import java.awt.geom.Rectangle2D;

public record BoundingBox(float x, float y, float width, float height) {
    private static final float Y_OVERLAP_THRESHOLD = 0.40f;

    public BoundingBox {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Dimensions cannot be negative: w=" + width + ", h=" + height);        }
        }

    public Rectangle2D.Float toAwtRectangle() {
        return new Rectangle2D.Float(x, y, width, height);
    }

    public BoundingBox union(BoundingBox other) {
        float newX = Math.min(this.x, other.x);
        float newY = Math.min(this.y, other.y);

        float maxX = Math.max(this.x + this.width, other.x + other.width);
        float maxY = Math.max(this.y + this.height, other.y + other.height);

        return new BoundingBox(newX, newY, maxX - newX, maxY - newY);
    }

    public float getRightX() { return x + width; }

    public float getBottomY() { return y + height; }

    public float horizontalGap(BoundingBox other) {
        return Math.max(0, other.x - this.getRightX());
    }

    public float verticalGap(BoundingBox other) {
        return Math.max(0, other.y - this.getBottomY());
    }

    public float verticalOverlapRatio(BoundingBox other) {
        float maxTop = Math.max(this.y, other.y);
        float minBottom = Math.min(this.getBottomY(), other.getBottomY());

        float overlapHeight = Math.max(0, minBottom - maxTop);
        float minHeight = Math.min(this.height, other.height);

        if (minHeight <= 0) return 0;
        return overlapHeight / minHeight;
    }

    public boolean overlapsHorizontally(BoundingBox other) {
        return this.x < other.getRightX() && this.getRightX() > other.x;
    }

    public static int compareReadingOrder(BoundingBox b1, BoundingBox b2) {
        float overlap = b1.verticalOverlapRatio(b2);
        boolean onSameLine = overlap > Y_OVERLAP_THRESHOLD;

        if (onSameLine) {
            return Float.compare(b1.x, b2.x);
        }
        return Float.compare(b1.y, b2.y);
    }
}
