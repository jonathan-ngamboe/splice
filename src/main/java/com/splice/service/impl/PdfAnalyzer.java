package com.splice.service.impl;

import com.splice.model.PageContent;

import com.splice.service.DocumentAnalyzer;
import com.splice.service.pdf.PageContentExtractor;
import org.apache.pdfbox.Loader;

import java.io.IOException;
import java.nio.file.Path;

import java.util.LinkedList;
import java.util.List;

public class PdfAnalyzer implements DocumentAnalyzer {
    @Override
    public boolean supports(Path path) {
        String filename = path.getFileName().toString().toLowerCase();
        return filename.endsWith(".pdf");
    }

    @Override
    public List<PageContent> analyze(Path path) throws IOException {
        List<PageContent> documentContent = new LinkedList<>();
        var file = path.toFile();

        try (var document = Loader.loadPDF(file)) {
            var pages = document.getPages();

            for (int i = 0; i < pages.getCount(); i++) {
                int pageNumber = i + 1;

                PageContentExtractor extractor = new PageContentExtractor(pageNumber);
                List<PageContent> pageResults = extractor.extract(document);
                documentContent.addAll(pageResults);
            }
        }
        return documentContent;
    }
}