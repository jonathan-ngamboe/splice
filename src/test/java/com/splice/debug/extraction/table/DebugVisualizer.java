package com.splice.debug.extraction.table;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import technology.tabula.Rectangle;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to visualize detection results on PDF pages.
 * <p>
 * Usage example:
 * <pre>
 * new DebugVisualizer(pdfPath)
 * .onPage(0)
 * .withScale(2.0f)
 * .draw(projectionResults, Color.MAGENTA, "Projection")
 * .draw(latticeResults, Color.RED, "Lattice")
 * .saveTo(outputDir, "debug_analysis");
 * </pre>
 */
public class DebugVisualizer {

    private final Path pdfPath;
    private int pageIndex = 0;
    private float scale = 2.0f;
    private final List<Layer> layers = new ArrayList<>();

    public DebugVisualizer(Path pdfPath) {
        this.pdfPath = pdfPath;
    }

    /**
     * Selects the page to render (0-based index).
     */
    public DebugVisualizer onPage(int pageIndex) {
        this.pageIndex = pageIndex;
        return this;
    }

    /**
     * Sets the zoom scale for the output image (1.0 = 72 DPI, 2.0 = 144 DPI).
     */
    public DebugVisualizer withScale(float scale) {
        this.scale = scale;
        return this;
    }

    /**
     * Adds a list of rectangles to be drawn.
     * Uses a default semi-transparent fill and solid stroke.
     *
     * @param rects The rectangles to draw (Tabula coordinates)
     * @param color The base color (transparency is handled automatically)
     */
    public DebugVisualizer draw(List<Rectangle> rects, Color color) {
        return draw(rects, color, null);
    }

    /**
     * Adds a list of rectangles with a specific label (optional).
     */
    public DebugVisualizer draw(List<Rectangle> rects, Color color, String label) {
        if (rects != null && !rects.isEmpty()) {
            this.layers.add(new Layer(rects, color, label));
        }
        return this;
    }

    /**
     * Renders the PDF page and the overlays, then saves to disk.
     *
     * @param outputDir Directory where the image will be saved
     * @param baseName  Base name for the file (e.g., "table_debug")
     */
    public void saveTo(Path outputDir, String baseName) {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
                System.err.println("Page index " + pageIndex + " is out of bounds for " + pdfPath);
                return;
            }

            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImage(pageIndex, scale);

            Graphics2D g2d = image.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            for (Layer layer : layers) {
                drawLayer(g2d, layer);
            }

            g2d.dispose();

            if (!outputDir.toFile().exists()) {
                outputDir.toFile().mkdirs();
            }

            String filename = String.format("%s_p%d.png", baseName, pageIndex + 1);
            Path outputPath = outputDir.resolve(filename);

            ImageIO.write(image, "PNG", outputPath.toFile());
            System.out.println("Debug image saved: " + outputPath.toAbsolutePath());

        } catch (IOException e) {
            throw new RuntimeException("Failed to save debug image", e);
        }
    }

    private void drawLayer(Graphics2D g2d, Layer layer) {
        Color strokeColor = layer.color;
        Color fillColor = new Color(strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue(), 50); // 50/255 alpha

        g2d.setStroke(new BasicStroke(2.0f * scale));

        for (Rectangle r : layer.rects) {
            int x = (int) (r.getX() * scale);
            int y = (int) (r.getY() * scale);
            int w = (int) (r.getWidth() * scale);
            int h = (int) (r.getHeight() * scale);

            g2d.setColor(fillColor);
            g2d.fillRect(x, y, w, h);

            g2d.setColor(strokeColor);
            g2d.drawRect(x, y, w, h);

            if (layer.label != null) {
                g2d.drawString(layer.label, x, y - 5);
            }
        }
    }

    private static class Layer {
        final List<Rectangle> rects;
        final Color color;
        final String label;

        Layer(List<Rectangle> rects, Color color, String label) {
            this.rects = rects;
            this.color = color;
            this.label = label;
        }
    }
}