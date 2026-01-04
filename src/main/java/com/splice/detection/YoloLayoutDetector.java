package com.splice.detection;

import com.splice.model.layout.PageLayout;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.translator.YoloV5Translator;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import ai.djl.MalformedModelException;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class YoloLayoutDetector implements LayoutDetector, AutoCloseable {
    private static final String MODEL_PATH = "/ml/models/yolov8x-doclaynet.onnx";

    private final ZooModel<Image, DetectedObjects> model;

    public YoloLayoutDetector() throws ModelNotFoundException, MalformedModelException, IOException {
        var criteria = loadCriteria();
        this.model = criteria.loadModel();
    }

    @Override
    public PageLayout detect(BufferedImage javaImage) throws TranslateException, ModelNotFoundException, MalformedModelException, IOException {
        if(javaImage == null) return null;

        Image djlImage = ImageFactory.getInstance().fromImage(javaImage);

        try (var predictor = model.newPredictor()) {

            DetectedObjects results = predictor.predict(djlImage);
            System.out.println("Result: " + results);

        }
        return null;
    }

    @Override
    public void close() {
        if (this.model != null) {
            this.model.close();
        }
    }

    private Criteria<Image, DetectedObjects> loadCriteria() {
        URL modelUrl = this.getClass().getResource(MODEL_PATH);

        if (modelUrl == null) {
            throw new IllegalStateException("CRITICAL: ONNX model not found in classpath at: [" + MODEL_PATH + "]");
        }

        return Criteria.builder()
                .setTypes(Image.class, DetectedObjects.class)
                .optModelUrls(modelUrl.toString())
                .optEngine("OnnxRuntime")
                .optTranslator(YoloV5Translator.builder().build())
                .build();
    }
}
