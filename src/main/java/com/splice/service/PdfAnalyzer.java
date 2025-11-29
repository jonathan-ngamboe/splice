package com.splice.service;

import com.splice.model.PageContent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class PdfAnalyzer implements DocumentAnalyzer{
    @Override
    public List<PageContent> analyze(Path path) throws IOException {
        return List.of();
    }
}