package com.splice.service.core;

import com.splice.model.PageContent;

import com.splice.service.DocumentAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class BatchProcessor {

    private static final Logger logger = LoggerFactory.getLogger(BatchProcessor.class);
    private final DocumentAnalyzer analyzer;

    public BatchProcessor(DocumentAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    /**
     * Ingests all PDF files in the given directory.
     *
     * @param directory The root path to scan.
     * @return A list of all extracted content.
     */
    public List<PageContent> ingestDirectory(Path directory, boolean recursive) {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Invalid directory path: " + directory);
        }

        int maxDepth = recursive ? Integer.MAX_VALUE : 1;

        try (var stream = Files.walk(directory, maxDepth)) {
            List<Path> filesToProcess = stream
                    .filter(Files::isRegularFile)
                    .filter(analyzer::supports)
                    .toList();

            if (filesToProcess.isEmpty()) {
                logger.info("No supported files found in directory: {}", directory);
                return Collections.emptyList();
            }

            logger.info("Starting analysis. Directory: {}, Files: {}", directory, filesToProcess.size());

            return processFiles(filesToProcess);

        } catch (IOException e) {
            logger.error("Fatal error scanning directory: {}", directory, e);
            throw new RuntimeException("Directory scanning failed", e);
        }
    }

    private List<PageContent> processFiles(List<Path> files) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            var tasks = files.stream()
                    .map(file -> (Callable<List<PageContent>>) () -> processSingleFile(file))
                    .toList();

            List<Future<List<PageContent>>> futures = executor.invokeAll(tasks);

            return aggregateResults(futures);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Batch processing was interrupted during execution", e);
            return Collections.emptyList();
        }
    }

    private List<PageContent> processSingleFile(Path file) {
        try {
            MDC.put("file_name", file.getFileName().toString());
            MDC.put("file_size", String.valueOf(Files.size(file)));

            logger.debug("Start processing file");

            long start = System.currentTimeMillis();
            List<PageContent> result = analyzer.analyze(file);

            logger.debug("Finished file: {} in {}ms", file.getFileName(), System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            logger.error("Failed to analyze file at path: {}", file.toAbsolutePath(), e);
            return Collections.emptyList();
        } finally {
            MDC.clear();
        }
    }

    private List<PageContent> aggregateResults(List<Future<List<PageContent>>> futures) {
        List<PageContent> results = new ArrayList<>();

        for (var future : futures) {
            try {
                results.addAll(future.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Aggregation interrupted");
            } catch (ExecutionException e) {
                logger.error("Unexpected error in concurrent task", e.getCause());
            }
        }

        logger.info("Batch completed. Total pages extracted: {}", results.size());
        return results;
    }
}