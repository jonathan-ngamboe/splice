package com.splice.service.pdf;

import com.splice.model.*;
import com.splice.util.PdfOperators;

import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import javax.imageio.*;
import java.io.*;

import java.util.*;

public class PageContentExtractor extends PDFTextStripper {
    private final List<PageContent> pageContents = new LinkedList<>();
    private final int pageNumber;

    public PageContentExtractor(int pageNumber) {
        super();
        this.pageNumber = pageNumber;
        this.setSortByPosition(false);
        this.setStartPage(pageNumber);
        this.setEndPage(pageNumber);
    }

    public List<PageContent> extract(PDDocument document) throws IOException {
        this.pageContents.clear();
        this.writeText(document, new StringWriter());
        return new LinkedList<>(this.pageContents);
    }

    @Override
    protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
        String operation = operator.getName();

        // Image Detection
        if (PdfOperators.DO_XOBJECT.equals(operation)) {
            processXObject(operands);
        }

        // Text Detection
        super.processOperator(operator, operands);
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) {
        if (!text.trim().isEmpty()) {
            pageContents.add(new TextContent(text, pageNumber));
        }
    }

    private void processXObject(List<COSBase> operands) throws IOException {
        if (operands.isEmpty() || !(operands.getFirst() instanceof COSName objectName)) {
            return;
        }

        PDXObject xobject = getResources().getXObject(objectName);

        if (xobject instanceof PDImageXObject imageXObject) {
            byte[] imageBytes;

            var stream = imageXObject.getStream();
            List<COSName> filters = stream.getFilters();

            if (filters != null && filters.contains(COSName.DCT_DECODE)) {
                try (InputStream in = stream.createInputStream()) {
                    imageBytes = in.readAllBytes();
                }
            }
            else {
                var bufferedImage = imageXObject.getImage();
                var suffix = imageXObject.getSuffix();
                if (suffix == null) suffix = "png";

                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    ImageIO.write(bufferedImage, suffix, out);
                    imageBytes = out.toByteArray();
                }
            }

            pageContents.add(new ImageContent(imageBytes, pageNumber));
        }
    }
}