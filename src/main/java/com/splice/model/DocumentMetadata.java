package com.splice.model;

public record DocumentMetadata(
    String filename,
    String fileHash,
    int totalPages,
    long processingTimeMs
) {}