package com.splice.model.document;

public record DocumentMetadata(
    String filename,
    String fileHash,
    int totalPages,
    long processingTimeMs
) {}