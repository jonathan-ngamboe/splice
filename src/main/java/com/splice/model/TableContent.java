package com.splice.model;

public record TableContent(
    String csvData
) implements PageContent {}
