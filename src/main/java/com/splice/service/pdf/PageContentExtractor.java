package com.splice.service.pdf;

import com.splice.model.ImageContent;
import com.splice.model.PageContent;
import com.splice.model.TextContent;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

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
        // Image Detection
        String operation = operator.getName();
        if ("Do".equals(operation)) {
            COSName objectName = (COSName) operands.getFirst();
            PDXObject xobject = getResources().getXObject(objectName);

            if (xobject instanceof PDImageXObject) {
                pageContents.add(new ImageContent(new byte[0], pageNumber));
            }
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
}