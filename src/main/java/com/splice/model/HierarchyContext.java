package com.splice.model;

public record HierarchyContext(
    String parentSectionId,
    String parentSectionTitle,
    int hierarchyLevel,
    boolean isContinuation
) {}