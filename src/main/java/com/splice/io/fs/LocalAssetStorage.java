package com.splice.io.fs;

import com.splice.extraction.spi.AssetStorage;
import com.splice.io.PathResolver;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class LocalAssetStorage implements AssetStorage {
    private final Path rootDirectory;
    private final PathResolver pathResolver;

    public LocalAssetStorage(Path rootDirectory) {
        this(rootDirectory, new PathResolver());
    }

    public LocalAssetStorage(Path rootDirectory, PathResolver pathResolver) {
        this.rootDirectory = rootDirectory;
        this.pathResolver = pathResolver;
        initDir();
    }

    private void initDir() {
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Could not create asset directory", e);
        }
    }

    @Override
    public String store(PDImageXObject image, String contextPrefix) throws IOException {
        String suffix = image.getSuffix();
        if (suffix == null) suffix = "png";

        String fileName = contextPrefix + "_" + UUID.randomUUID().toString().substring(0, 8);

        Path targetPath = pathResolver.resolveUniquePath(rootDirectory, fileName, "." + suffix);

        BufferedImage bufferedImage = image.getImage();
        ImageIO.write(bufferedImage, suffix, targetPath.toFile());

        return targetPath.toAbsolutePath().toString();
    }
}