package com.splice.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.splice.model.PageContent;
import com.splice.model.TextContent;
import com.splice.service.DocumentAnalyzer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;

public class BatchProcessorTests {
    @TempDir
    Path tempDir;

    DocumentAnalyzer fakeAnalyzer = new DocumentAnalyzer() {
        @Override
        public boolean supports(Path path) {
            return path.toString().endsWith(".pdf");
        }

        @Override
        public List<PageContent> analyze(Path path) throws IOException {
            if (path.toString().endsWith("crash.pdf")) {
                throw new IOException("Simulated corrupted file");
            }
            return List.of(new TextContent("Simulated content", 1));
        }
    };

    private final BatchProcessor processor = new BatchProcessor(fakeAnalyzer);


    @Test
    public void ingestDirectory_validPdfOnly_shouldAggregateResults() throws IOException {
        Files.createFile(tempDir.resolve("doc1.pdf"));
        Files.createFile(tempDir.resolve("doc2.pdf"));

        var results = processor.ingestDirectory(tempDir);

        assertEquals(2, results.size(), "Should contain the pages from both files");
    }

    @Test
    public void ingestDirectory_mixedFiles_shouldIgnoreNonPdf() throws IOException {
        Files.createFile(tempDir.resolve("doc1.pdf"));
        Files.createFile(tempDir.resolve("notes.txt"));

        var results = processor.ingestDirectory(tempDir);

        assertEquals(1, results.size(), "The .txt file should be ignored.");
    }

    @Test
    public void ingestDirectory_corruptedFile_shouldContinueProcessingOthers() throws IOException {
        Files.createFile(tempDir.resolve("good.pdf"));
        Files.createFile(tempDir.resolve("bad_crash.pdf"));

        var results = processor.ingestDirectory(tempDir);

        assertEquals(1, results.size(), "The process should not crash completely; it should recover the correct file.");    }
}
