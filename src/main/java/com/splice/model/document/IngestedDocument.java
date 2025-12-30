package com.splice.model.document;

import java.util.List;

public record IngestedDocument(
    String id,
    DocumentMetadata metadata,
    List<DocumentElement> elements
) {}