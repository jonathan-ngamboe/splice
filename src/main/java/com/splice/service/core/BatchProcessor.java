package com.splice.service.core;

import com.splice.io.PathResolver;
import com.splice.model.IngestedDocument;
import com.splice.service.DocumentAnalyzer;
import com.splice.service.ResultWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchProcessor {

    private static final Logger logger = LoggerFactory.getLogger(BatchProcessor.class);

    private final DocumentAnalyzer analyzer;
    private final ResultWriter writer;
    private final PathResolver pathResolver;

    public BatchProcessor(DocumentAnalyzer analyzer, ResultWriter writer) {
        this(analyzer, writer, new PathResolver());
    }

    public BatchProcessor(DocumentAnalyzer analyzer, ResultWriter writer, PathResolver pathResolver) {
        this.analyzer = analyzer;
        this.writer = writer;
        this.pathResolver = pathResolver;
    }

    /**
     * Ingests documents from the specified directory using a streaming approach.
     * Files are processed concurrently, and results are written immediately to disk to save memory.
     *
     * @param inputDirectory  The root path containing the documents to process.
     * @param outputDirectory The directory where JSON reports will be saved.
     * @param recursive       {@code true} to include subdirectories; {@code false} to process only the top-level directory.
     * @throws RuntimeException If an I/O error occurs during file traversal or directory creation.
     */
    public void process(Path inputDirectory, Path outputDirectory, boolean recursive) {
        validateInputs(inputDirectory, outputDirectory);

        int maxDepth = recursive ? Integer.MAX_VALUE : 1;
        AtomicInteger totalPages = new AtomicInteger(0);

        try (var stream = Files.walk(inputDirectory, maxDepth);
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<Path> filesToProcess = stream
                    .filter(Files::isRegularFile)
                    .filter(analyzer::supports)
                    .toList();

            if (filesToProcess.isEmpty()) {
                logger.info("No supported files found in: {}", inputDirectory);
                return;
            }

            logger.info("Batch started. Files: {}", filesToProcess.size());

            var tasks = filesToProcess.stream()
                    .map(file -> (Callable<Void>) () -> {
                        int count = processSingleFile(file, outputDirectory);
                        totalPages.addAndGet(count);
                        return null;
                    })
                    .toList();

            executor.invokeAll(tasks);

            logger.info("Batch completed. Pages processed: {}", totalPages.get());

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            logger.error("Processing failed", e);
            throw new RuntimeException("Batch processing failed", e);
        }
    }

    private int processSingleFile(Path inputFile, Path outputDirectory) {
        try {
            MDC.put("file", inputFile.getFileName().toString());
            long start = System.currentTimeMillis();

            IngestedDocument result = analyzer.analyze(inputFile);

            Path targetFile = pathResolver.resolveUniquePath(
                    outputDirectory,
                    inputFile.getFileName().toString(),
                    writer.extension()
            );

            writer.write(result, targetFile);

            logger.debug("Processed in {}ms -> {}", System.currentTimeMillis() - start, targetFile.getFileName());
            return result.metadata().totalPages();

        } catch (Exception e) {
            logger.error("Failed to process file", e);
            return 0;
        } finally {
            MDC.clear();
        }
    }

    private void validateInputs(Path input, Path output) {
        if (!Files.isDirectory(input)) throw new IllegalArgumentException("Invalid input: " + input);
        try {
            Files.createDirectories(output);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create output: " + output, e);
        }
    }
}