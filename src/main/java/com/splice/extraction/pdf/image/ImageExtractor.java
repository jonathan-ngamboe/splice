package com.splice.extraction.pdf.image;

import com.splice.extraction.spi.AssetStorage;
import com.splice.model.document.*;
import com.splice.model.document.content.ImageContent;
import com.splice.model.geometry.BoundingBox;

import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.OperatorName;
import org.apache.pdfbox.contentstream.operator.state.Concatenate;
import org.apache.pdfbox.contentstream.operator.state.Restore;
import org.apache.pdfbox.contentstream.operator.state.Save;
import org.apache.pdfbox.contentstream.operator.state.SetMatrix;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;

import java.io.IOException;
import java.util.*;

public class ImageExtractor extends PDFStreamEngine {
    private static final float MIN_DISPLAY_WIDTH = 50.0f;
    private static final float MIN_DISPLAY_HEIGHT = 50.0f;

    private final AssetStorage imageStorage;
    private final List<DocumentElement> extractedImages = new ArrayList<>();

    private int currentPageNumber;
    private float currentPageHeight;

    public ImageExtractor(AssetStorage imageStorage) {
        this.imageStorage = imageStorage;

        addOperator(new Concatenate(this));
        addOperator(new Save(this));
        addOperator(new Restore(this));
        addOperator(new SetMatrix(this));
    }

    public List<DocumentElement> extract(PDPage page, int pageNumber) throws IOException {
        if(page == null) return List.of();

        this.extractedImages.clear();
        this.currentPageNumber = pageNumber;
        this.currentPageHeight = page.getCropBox().getHeight();

        processPage(page);

        return new ArrayList<>(this.extractedImages);
    }

    @Override
    protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
        String operation = operator.getName();
        if (OperatorName.DRAW_OBJECT.equals(operation)) {
            COSName objectName = (COSName) operands.getFirst();
            PDXObject xobject = getResources().getXObject(objectName);

            if (xobject instanceof PDImageXObject image) {
                processImageInstance(image);
            } else if (xobject instanceof PDFormXObject form) {
                showForm(form);
            }
        } else {
            super.processOperator(operator, operands);
        }
    }

    private void processImageInstance(PDImageXObject image) throws IOException {
        Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();

        float displayWidth = ctm.getScalingFactorX();
        float displayHeight = ctm.getScalingFactorY();
        float xPdf = ctm.getTranslateX();
        float yPdf = ctm.getTranslateY();

        if (displayWidth < MIN_DISPLAY_WIDTH || displayHeight < MIN_DISPLAY_HEIGHT) {
            return;
        }

        String storedPath = imageStorage.store(image, String.valueOf(currentPageNumber));

        float yWeb = currentPageHeight - yPdf - displayHeight;

        BoundingBox box = new BoundingBox(xPdf, yWeb, displayWidth, displayHeight);

        extractedImages.add(new DocumentElement(
                UUID.randomUUID().toString(),
                ElementType.IMAGE,
                new Location(currentPageNumber, box),
                null, // Will be extracted separately
                new ImageContent(storedPath, null)
        ));
    }
}