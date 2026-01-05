package com.splice.pipeline;

import com.splice.extraction.pdf.PdfExtractor;
import com.splice.extraction.spi.ExtractorProvider;
import com.splice.io.PathResolver;
import com.splice.io.fs.LocalAssetStorage;
import com.splice.model.document.IngestedDocument;
import com.splice.extraction.DocumentExtractor;
import com.splice.io.ResultWriter;

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
import java.util.stream.Stream;

public class BatchProcessor {

    private static final Logger logger = LoggerFactory.getLogger(BatchProcessor.class);

    private final ResultWriter writer;
    private final PathResolver pathResolver;
    private final List<ExtractorProvider> providers;
    private final com.splice.detection.LayoutDetector detector;

    public BatchProcessor(ResultWriter writer, List<ExtractorProvider> providers, com.splice.detection.LayoutDetector detector) {
        this(writer, new PathResolver(), providers, detector);
    }

    public BatchProcessor(ResultWriter writer, PathResolver pathResolver, List<ExtractorProvider> providers, com.splice.detection.LayoutDetector detector) {
        this.writer = writer;
        this.pathResolver = pathResolver;
        this.providers = providers;
        this.detector = detector;
    }

    /**
     * Ingests documents from the specified directory using a streaming approach.
     * Files are processed concurrently, and results are written immediately to disk to save memory.
     *
     * @param inputRoot  The root path containing the documents to process.
     * @param outputRoot The directory where JSON reports will be saved.
     * @param recursive       {@code true} to include subdirectories; {@code false} to process only the top-level directory.
     * @throws RuntimeException If an I/O error occurs during file traversal or directory creation.
     */
    public void process(Path inputRoot, Path outputRoot, boolean recursive) {
        validateInputs(inputRoot, outputRoot);

        int maxDepth = recursive ? Integer.MAX_VALUE : 1;
        AtomicInteger totalPages = new AtomicInteger(0);

        try (Stream<Path> stream = Files.walk(inputRoot, maxDepth);
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<Path> filesToProcess = stream
                    .filter(Files::isRegularFile)
                    .filter(this::isSupported)
                    .toList();

            if (filesToProcess.isEmpty()) {
                logger.info("No supported files found in: {}", inputRoot);
                return;
            }

            logger.info("Batch started. Files: {}", filesToProcess.size());

            var tasks = filesToProcess.stream()
                    .map(file -> (Callable<Void>) () -> {
                        int count = processSingleFile(file, inputRoot, outputRoot);                        totalPages.addAndGet(count);
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

    private boolean isSupported(Path path) {
        return providers.stream().anyMatch(p -> p.supports(path));
    }

    private int processSingleFile(Path inputFile, Path inputRoot, Path outputRoot) {
        try {
            MDC.put("file", inputFile.getFileName().toString());
            long start = System.currentTimeMillis();

            ExtractorProvider provider = providers.stream()
                    .filter(p -> p.supports(inputFile))
                    .findFirst()
                    .orElseThrow();

            Path relativePath = inputRoot.relativize(inputFile.getParent());
            Path targetDir = outputRoot.resolve(relativePath);
            Files.createDirectories(targetDir);

            String baseName = getFileNameWithoutExtension(inputFile);
            Path specificAssetDir = targetDir.resolve(baseName + "_assets");

            var assetStorage = new LocalAssetStorage(specificAssetDir);

            DocumentExtractor extractor = provider.create(assetStorage, detector);

            IngestedDocument result = extractor.extract(inputFile);

            Path targetJsonFile = pathResolver.resolveUniquePath(
                    targetDir,
                    inputFile.getFileName().toString(),
                    writer.extension()
            );

            writer.write(result, targetJsonFile);

            logger.debug("Processed in {}ms -> {}", System.currentTimeMillis() - start, targetJsonFile);
            return result.metadata().totalPages();

        } catch (Exception e) {
            logger.error("Failed to process file: {}", inputFile, e);
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

    private String getFileNameWithoutExtension(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }
}