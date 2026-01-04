package com.splice.model.document;

public enum ElementType {
    CAPTION("Caption"),
    FOOTNOTE("Footnote"),
    FORMULA("Formula"),
    LIST_ITEM("List-item"),
    PAGE_FOOTER("Page-footer"),
    PAGE_HEADER("Page-header"),
    IMAGE("Picture"),
    SECTION_HEADER("Section-header"),
    TABLE("Table"),
    TEXT("Text"),
    TITLE("Title"),
    UNKNOWN("Unknown");

    private final String modelLabel;

    ElementType(String modelLabel) {
        this.modelLabel = modelLabel;
    }

    public static ElementType fromLabel(String label) {
        if (label == null) return UNKNOWN;

        for (ElementType type : values()) {
            if (type.modelLabel.equalsIgnoreCase(label)) {
                return type;
            }
        }

        return UNKNOWN;
    }
}