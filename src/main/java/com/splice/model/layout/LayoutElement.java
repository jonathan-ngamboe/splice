package com.splice.model.layout;

import ai.djl.modality.cv.output.BoundingBox;
import com.splice.model.document.ElementType;

public record LayoutElement(
        ElementType type,
        BoundingBox box,
        double confidence
) {}