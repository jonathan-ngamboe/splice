package com.splice.extraction.pdf;

import ai.djl.MalformedModelException;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.translate.TranslateException;

import com.splice.detection.YoloLayoutDetector;
import com.splice.extraction.DocumentExtractor;
import com.splice.extraction.pdf.image.ImageExtractor;
import com.splice.extraction.pdf.table.TableExtractor;
import com.splice.extraction.pdf.text.TextExtractor;
import com.splice.extraction.spi.AssetStorage;
import com.splice.extraction.spi.ExtractorProvider;
import com.splice.model.document.*;
import com.splice.model.layout.PageLayout;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.rendering.PDFRenderer;

import technology.tabula.*;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.io.*;
import java.util.*;


public class PdfExtractor implements DocumentExtractor {
    private final AssetStorage assetStorage;
    private final YoloLayoutDetector yoloDetector;

    public PdfExtractor(AssetStorage assetStorage) {
        this.assetStorage = assetStorage;
        try {
            yoloDetector = new YoloLayoutDetector();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static final ExtractorProvider PROVIDER = new ExtractorProvider() {
        @Override
        public boolean supports(Path path) {
            return path.getFileName().toString().toLowerCase().endsWith(".pdf");
        }

        @Override
        public DocumentExtractor create(AssetStorage storage) {
            return new PdfExtractor(storage);
        }
    };

    @Override
    public IngestedDocument extract(Path path) throws IOException {
        var tableExtractor = new TableExtractor();
        var textExtractor = new TextExtractor();
        var imageExtractor = new ImageExtractor(assetStorage);

        List<DocumentElement> allElements = new ArrayList<>();
        DocumentMetadata metadata;
        File file = path.toFile();

        try (var document = Loader.loadPDF(file, IOUtils.createTempFileOnlyStreamCache());
             var tabulaExtractor = new ObjectExtractor(document)) {
            PageIterator tabulaIterator = tabulaExtractor.extract();

            int pageNumber = 1;
            long start = System.currentTimeMillis();

            while (tabulaIterator.hasNext()) {
                var tabulaPage = tabulaIterator.next();
                var standardPage = document.getPage(pageNumber - 1);

                var pdfRenderer = new PDFRenderer(document);
                BufferedImage img = pdfRenderer.renderImageWithDPI(pageNumber - 1, 72);

                PageLayout pageLayout = yoloDetector.detect(img, pageNumber);

                for(var layoutElement : pageLayout.elements()) {
                    var type = layoutElement.type();
                    var region = layoutElement.box();

                    switch (type) {
                        case IMAGE:
                            var imageElements = imageExtractor.extractRegion(standardPage, pageNumber, region);
                            allElements.addAll(imageElements);
                            System.out.println("Image: " + imageElements);
                            break;
                        case TABLE:
                            var tableElements = tableExtractor.extractRegion(tabulaPage, region);
                            allElements.addAll(tableElements);
                            System.out.println("Table: " + tableElements);
                            break;
                        default:
                            var textElements = textExtractor.extractRegion(document, pageNumber, region, layoutElement);
                            System.out.println("Text: " + textElements);
                            allElements.addAll(textElements);
                    }
                }
                pageNumber++;
            }

            allElements.sort(DocumentElement.READING_ORDER);

            long duration = System.currentTimeMillis() - start;
            System.out.println("Duration: " + duration);

            metadata = new DocumentMetadata(
                getFileName(path),
                calculateFileHash(path),
                document.getNumberOfPages(),
                duration
            );
        } catch (TranslateException | ModelNotFoundException | MalformedModelException e) {
            throw new RuntimeException(e);
        }
        return new IngestedDocument(UUID.randomUUID().toString(), metadata, allElements);
    }

    private String getFileName(Path path) {
        return path.getFileName().toString().toLowerCase();
    }

}