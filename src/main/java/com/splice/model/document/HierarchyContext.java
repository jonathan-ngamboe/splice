package com.splice.model.document;

public record HierarchyContext(
    String parentSectionId,
    String parentSectionTitle,
    int hierarchyLevel,
    boolean isContinuation
) {}