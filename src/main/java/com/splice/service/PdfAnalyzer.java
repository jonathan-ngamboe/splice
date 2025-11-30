package com.splice.service;

import com.splice.model.PageContent;
import com.splice.model.TextContent;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Path;

import java.util.LinkedList;
import java.util.List;

public class PdfAnalyzer implements DocumentAnalyzer{
    @Override
    public List<PageContent> analyze(Path path) throws IOException {
        List<PageContent> documentContent = new LinkedList<>();

        var file = path.toFile();
        try (var document = Loader.loadPDF(file)) {
            var pages = document.getPages();

            for (int pageNumber = 1; pageNumber <= pages.getCount(); pageNumber++) {
                String pageText = getPageText(document, pageNumber);
                PageContent content = new TextContent(pageText, pageNumber);
                documentContent.add(content);
            }
        }

        return documentContent;
    }

    private String getPageText(PDDocument doc, int page) throws IOException {
        var stripper = new PDFTextStripper();

        stripper.setStartPage(page);
        stripper.setEndPage(page);

        return stripper.getText(doc);
    }
}