package com.splice.geometry;

import java.awt.geom.Rectangle2D;

public record BoundingBox(float x, float y, float width, float height) {
    public BoundingBox {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Dimensions cannot be negative: w=" + width + ", h=" + height);        }
        }

    public Rectangle2D.Float toAwtRectangle() {
        return new Rectangle2D.Float(x, y, width, height);
    }
}
