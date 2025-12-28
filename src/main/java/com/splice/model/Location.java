package com.splice.model;

import com.splice.geometry.BoundingBox;

public record Location(
    int pageNumber,
    BoundingBox bbox
) {}
