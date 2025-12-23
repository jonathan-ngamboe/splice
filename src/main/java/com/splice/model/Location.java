package com.splice.model;

import org.apache.fontbox.util.BoundingBox;

public record Location(
    int pageNumber,
    BoundingBox bbox
) {}
