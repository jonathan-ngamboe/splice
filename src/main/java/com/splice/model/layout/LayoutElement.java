package com.splice.model.layout;

import com.splice.model.document.ElementType;
import com.splice.model.geometry.BoundingBox;

public record LayoutElement(
        double confidence,
        ElementType type,
        BoundingBox box
) {}