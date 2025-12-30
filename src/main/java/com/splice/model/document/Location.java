package com.splice.model.document;

import com.splice.model.geometry.BoundingBox;

public record Location(
    int pageNumber,
    BoundingBox bbox
) {}
