package com.splice.model.document;

import com.splice.model.document.content.PageContent;
import com.splice.model.geometry.BoundingBox;

import java.util.Comparator;

public record DocumentElement(
    String id,
    ElementType type,
    Location location,
    HierarchyContext context,
    PageContent content
) {
    public static final Comparator<DocumentElement> READING_ORDER = Comparator
            .comparing((DocumentElement e) -> e.location().pageNumber())
            .thenComparing((e1, e2) -> BoundingBox.compareReadingOrder(
                    e1.location().bbox(),
                    e2.location().bbox()
            ));
}
