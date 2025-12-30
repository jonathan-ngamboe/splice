package com.splice.extraction.pdf;

import com.splice.extraction.DocumentExtractor;

import com.splice.extraction.pdf.image.ImageExtractor;
import com.splice.extraction.pdf.table.TableExtractor;
import com.splice.extraction.pdf.text.TextExtractor;
import com.splice.extraction.spi.AssetStorage;
import com.splice.extraction.spi.ExtractorProvider;
import com.splice.model.document.*;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;

import technology.tabula.*;

import java.awt.geom.Rectangle2D;
import java.nio.file.Path;
import java.io.*;
import java.util.*;

public class PdfExtractor implements DocumentExtractor {
    private final AssetStorage assetStorage;

    public PdfExtractor(AssetStorage assetStorage) {
        this.assetStorage = assetStorage;
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

        try (PDDocument document = Loader.loadPDF(file, IOUtils.createTempFileOnlyStreamCache())) {

            var tabulaExtractor = new ObjectExtractor(document);
            PageIterator tabulaIterator = tabulaExtractor.extract();

            int pageNumber = 1;
            long start = System.currentTimeMillis();

            while (tabulaIterator.hasNext()) {
                var tabulaPage = tabulaIterator.next();
                var standardPage = document.getPage(pageNumber - 1);

                var tableElements = tableExtractor.extract(tabulaPage);
                var tableZones = getOccupiedAreas(tableElements);

                var textElements = textExtractor.extract(document, pageNumber, tableZones);

                var imageElements = imageExtractor.extract(standardPage, pageNumber);

                allElements.addAll(tableElements);
                allElements.addAll(textElements);
                allElements.addAll(imageElements);

                pageNumber++;
            }

            allElements.sort(DocumentElement.READING_ORDER);

            long duration = System.currentTimeMillis() - start;

            metadata = new DocumentMetadata(
                getFileName(path),
                calculateFileHash(path),
                document.getNumberOfPages(),
                duration
            );
        }
        return new IngestedDocument(UUID.randomUUID().toString(), metadata, allElements);
    }

    private List<Rectangle2D.Float> getOccupiedAreas(List<DocumentElement> content) {
        return content
                .stream()
                .map(c -> c.location().bbox().toAwtRectangle())
                .toList();
    }

    private String getFileName(Path path) {
        return path.getFileName().toString().toLowerCase();
    }

}