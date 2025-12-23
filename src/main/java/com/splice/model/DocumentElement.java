package com.splice.model;

public record DocumentElement(
    String id,
    ElementType type,
    Location location,
    HierarchyContext context,
    PageContent content
) {}
