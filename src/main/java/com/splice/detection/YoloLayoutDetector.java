package com.splice.detection;

import com.splice.model.document.ElementType;
import com.splice.model.geometry.BoundingBox;
import com.splice.model.layout.LayoutElement;
import com.splice.model.layout.PageLayout;

import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.translator.YoloV8Translator;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Pipeline;
import ai.djl.MalformedModelException;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class YoloLayoutDetector implements LayoutDetector, AutoCloseable {
    private static final String SYNSET_PATH = "/ml/models/synset.txt";
    private static final String MODEL_PATH = "/ml/models/yolov8x-doclaynet-quant.onnx";
    private static final float CONFIDENCE_THRESHOLD = 0.5f;
    private static final float NMS_THRESHOLD = 0.4f;
    private static final int INPUT_SIZE = 640;

    private final ZooModel<Image, DetectedObjects> model;

    public YoloLayoutDetector() throws ModelNotFoundException, MalformedModelException, IOException {
        var criteria = loadCriteria();
        this.model = criteria.loadModel();
    }

    @Override
    public PageLayout detect(BufferedImage javaImage, int pageNumber) throws TranslateException {
        if(javaImage == null) return null;

        List<LayoutElement> elements = new ArrayList<>();

        Image djlImage = ImageFactory.getInstance().fromImage(javaImage);

        int imgWidth = javaImage.getWidth();
        int imgHeight = javaImage.getHeight();

        try (var predictor = model.newPredictor()) {

            var detectedObjects = predictor.predict(djlImage).items();

            detectedObjects.stream()
                    .filter(DetectedObjects.DetectedObject.class::isInstance)
                    .map(DetectedObjects.DetectedObject.class::cast)
                    .forEach(
                            object -> elements.add(
                                    new LayoutElement(
                                            object.getProbability(),
                                            ElementType.fromLabel(object.getClassName()),
                                            transformToLocalBox(object.getBoundingBox(), imgWidth, imgHeight)
                                    )
                            )
                    );
        }

        return new PageLayout(pageNumber, elements);
    }

    @Override
    public void close() {
        if (this.model != null) {
            this.model.close();
        }
    }

    private BoundingBox transformToLocalBox(ai.djl.modality.cv.output.BoundingBox box, int imgWidth, int imgHeight) {
        var rect = box.getBounds();

        var scaleX = (double) imgWidth / INPUT_SIZE;
        var scaleY = (double) imgHeight / INPUT_SIZE;

        var x      = (float) (rect.getX() * scaleX);
        var y      = (float) (rect.getY() * scaleY);
        var width  = (float) (rect.getWidth() * scaleX);
        var height = (float) (rect.getHeight() * scaleY);

        return new BoundingBox(x, y, width, height);
    }

    private Criteria<Image, DetectedObjects> loadCriteria() throws IOException {
        Pipeline pipeline = new Pipeline();
        pipeline.add(new Resize(INPUT_SIZE, INPUT_SIZE));
        pipeline.add(new ToTensor());

        Path tempModelFile = Files.createTempFile("yolov8", ".onnx");
        tempModelFile.toFile().deleteOnExit();

        try (var modelInput = this.getClass().getResourceAsStream(MODEL_PATH)) {
            if (modelInput == null) {
                throw new IllegalStateException("CRITICAL: Model not found at: " + MODEL_PATH);
            }
            Files.copy(modelInput, tempModelFile, StandardCopyOption.REPLACE_EXISTING);
        }

        try (var synsetInput = this.getClass().getResourceAsStream(SYNSET_PATH)) {
            if (synsetInput != null) {
                Path tempSynsetFile = tempModelFile.getParent().resolve("synset.txt");
                tempSynsetFile.toFile().deleteOnExit();
                Files.copy(synsetInput, tempSynsetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        String modelUrl = tempModelFile.toUri().toString();

        return Criteria.builder()
                .setTypes(Image.class, DetectedObjects.class)
                .optModelUrls(modelUrl)
                .optEngine("OnnxRuntime")
                .optTranslator(YoloV8Translator.builder()
                        .setPipeline(pipeline)
                        .optThreshold(CONFIDENCE_THRESHOLD)
                        .optNmsThreshold(NMS_THRESHOLD)
                        .build())
                .build();
    }
}