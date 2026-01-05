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

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.*;

public class ImageExtractor extends PDFStreamEngine {
    private static final float MIN_DISPLAY_WIDTH = 50.0f;
    private static final float MIN_DISPLAY_HEIGHT = 50.0f;

    private final AssetStorage imageStorage;
    private final List<DocumentElement> extractedImages = new ArrayList<>();

    private int currentPageNumber;
    private float currentPageHeight;
    private BoundingBox regionOfInterest;

    public ImageExtractor(AssetStorage imageStorage) {
        this.imageStorage = imageStorage;

        addOperator(new Concatenate(this));
        addOperator(new Save(this));
        addOperator(new Restore(this));
        addOperator(new SetMatrix(this));
    }

    public List<DocumentElement> extract(PDPage page, int pageNumber) throws IOException {
        return extractRegion(page, pageNumber, null);
    }

    public List<DocumentElement> extractRegion(PDPage page, int pageNumber, BoundingBox region) throws IOException {
        if(page == null) return List.of();

        this.extractedImages.clear();
        this.currentPageNumber = pageNumber;
        this.currentPageHeight = page.getCropBox().getHeight();
        this.regionOfInterest = region;

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

        float yWeb = currentPageHeight - yPdf - displayHeight;
        BoundingBox imageBox = new BoundingBox(xPdf, yWeb, displayWidth, displayHeight);

        if (this.regionOfInterest != null) {
            Rectangle2D.Float regionRect = new Rectangle2D.Float(
                    regionOfInterest.x(), regionOfInterest.y(),
                    regionOfInterest.width(), regionOfInterest.height()
            );

            Rectangle2D.Float imgRect = new Rectangle2D.Float(
                    imageBox.x(), imageBox.y(),
                    imageBox.width(), imageBox.height()
            );

            if (!regionRect.intersects(imgRect)) {
                return;
            }
        }

        String storedPath = imageStorage.store(image, String.valueOf(currentPageNumber));

        extractedImages.add(new DocumentElement(
                UUID.randomUUID().toString(),
                ElementType.IMAGE,
                new Location(currentPageNumber, imageBox),
                null, // Will be extracted separately
                new ImageContent(storedPath, null)
        ));
    }
}