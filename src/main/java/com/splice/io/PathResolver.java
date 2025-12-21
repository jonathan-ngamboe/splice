package com.splice.io;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;


public class PathResolver {
    public Path resolveUniquePath(Path outputDirectory, String originalFileName, String extension) throws IOException {
        String baseName = removeExtension(originalFileName);

        Path candidate = outputDirectory.resolve(baseName + extension);
        if (tryReserveFile(candidate)) {
            return candidate;
        }

        int counter = 1;
        while (true) {
            String newName = baseName + "_" + counter + extension;
            candidate = outputDirectory.resolve(newName);

            if (tryReserveFile(candidate)) {
                return candidate;
            }
            counter++;
        }
    }

    private boolean tryReserveFile(Path path) {
        try {
            Files.createFile(path);
            return true;
        } catch (FileAlreadyExistsException e) {
            return false;
        } catch (IOException e) {
            throw new RuntimeException("Disk error while reserving the file: " + path, e);
        }
    }

    private String removeExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return (lastDot > 0) ? filename.substring(0, lastDot) : filename;
    }
}