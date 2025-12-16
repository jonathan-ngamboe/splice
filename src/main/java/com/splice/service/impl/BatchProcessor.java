package com.splice.service.impl;

import com.splice.model.PageContent;
import com.splice.service.DocumentAnalyzer;

import java.nio.file.Path;
import java.util.List;

public class BatchProcessor {
    private final DocumentAnalyzer analyzer;

    public BatchProcessor(DocumentAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public List<PageContent> ingestDirectory(Path directory) {
        return List.of();
    }
}
