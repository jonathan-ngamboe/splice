package com.splice.model.layout;

import com.splice.model.document.ElementType;

import java.util.List;

public record PageLayout (int pageNumber, List<LayoutElement> elements) {
    public List<LayoutElement> getElementsByType(ElementType type) {
        return elements.stream()
                .filter(e -> e.type() == type)
                .toList();
    }
}
