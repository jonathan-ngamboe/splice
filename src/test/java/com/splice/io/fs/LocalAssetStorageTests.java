package com.splice.io.fs;

import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalAssetStorageTests {

    @TempDir
    Path tempDir;

    @Mock
    PDImageXObject mockImage;

    @Test
    @DisplayName("Should recursively create directory structure if it doesn't exist")
    void shouldCreateDirectoryRecursively() throws IOException {
        Path deepPath = tempDir.resolve("sub/folder/assets");
        LocalAssetStorage storage = new LocalAssetStorage(deepPath);
        setupMockImage(mockImage, "jpg");

        storage.store(mockImage, "img");

        assertTrue(Files.exists(deepPath), "Deeply nested directory should be created automatically");
        assertTrue(Files.isDirectory(deepPath));
    }

    @Test
    @DisplayName("Should store image and return absolute path")
    void shouldStoreImageAndReturnAbsolutePath() throws IOException {
        Path rootAssetDir = tempDir.resolve("assets");
        LocalAssetStorage storage = new LocalAssetStorage(rootAssetDir);
        setupMockImage(mockImage, "png");

        String resultPathString = storage.store(mockImage, "page_1_header");
        Path resultPath = Path.of(resultPathString);

        assertTrue(resultPath.isAbsolute(), "Returned path should be absolute for robust usage downstream");
        assertTrue(Files.exists(resultPath), "File should be written to disk");
        assertEquals(rootAssetDir, resultPath.getParent(), "File should be in the correct directory");
        assertTrue(resultPath.getFileName().toString().startsWith("page_1_header"), "Filename should contain the prefix context");
        assertTrue(Files.size(resultPath) > 0, "File content should not be empty");
    }

    @Test
    @DisplayName("Should handle missing suffix by defaulting to png")
    void shouldDefaultToPngWhenSuffixIsNull() throws IOException {
        LocalAssetStorage storage = new LocalAssetStorage(tempDir);
        setupMockImage(mockImage, null);

        String resultPath = storage.store(mockImage, "img");

        assertTrue(resultPath.endsWith(".png"), "Should default to .png extension when suffix is null");
    }

    @Test
    @DisplayName("Should generate unique filenames for same prefix")
    void shouldGenerateUniqueFilenames() throws IOException {
        LocalAssetStorage storage = new LocalAssetStorage(tempDir);
        setupMockImage(mockImage, "png");

        String path1 = storage.store(mockImage, "logo");
        String path2 = storage.store(mockImage, "logo");

        assertNotEquals(path1, path2, "Two calls with same prefix should result in different files (UUID collision check)");
        assertTrue(Files.exists(Path.of(path1)));
        assertTrue(Files.exists(Path.of(path2)));
    }

    @Test
    @DisplayName("Should throw RuntimeException when directory is not writable")
    void shouldThrowExceptionOnWriteError() throws IOException {
        File readOnlyDir = tempDir.resolve("readonly").toFile();

        if (readOnlyDir.mkdir() && readOnlyDir.setWritable(false, false)) {
            try {
                LocalAssetStorage storage = new LocalAssetStorage(readOnlyDir.toPath());
                setupMockImage(mockImage, "png");

                assertThrows(RuntimeException.class, () -> {
                    storage.store(mockImage, "fail");
                }, "Should throw exception if writing is impossible");
            } finally {
                readOnlyDir.setWritable(true, true);
            }
        } else {
            System.out.println("Skipping permission test: OS did not allow locking directory (Common on Windows/CI)");
        }
    }

    private void setupMockImage(PDImageXObject imageMock, String suffix) throws IOException {
        BufferedImage validImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);

        lenient().when(imageMock.getImage()).thenReturn(validImage);
        lenient().when(imageMock.getSuffix()).thenReturn(suffix);
    }
}