package com.splice.detection;

import com.splice.model.layout.PageLayout;

import java.awt.image.BufferedImage;

public interface LayoutDetector {
    PageLayout detect(BufferedImage javaImage, int pageNumber) throws Exception;
}
