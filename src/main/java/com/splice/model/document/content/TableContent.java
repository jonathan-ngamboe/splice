package com.splice.model.document.content;

public record TableContent(
    String csvData
) implements PageContent {}
