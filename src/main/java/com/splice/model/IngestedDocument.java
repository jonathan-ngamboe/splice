package com.splice.model;

import java.util.List;

public record IngestedDocument(
    String id,
    DocumentMetadata metadata,
    List<DocumentElement> elements
) {}