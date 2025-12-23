package com.splice.service.core;

import com.splice.model.IngestedDocument;
import com.splice.service.DocumentAnalyzer;
import com.splice.service.ResultWriter;
import com.splice.service.json.JsonResultWriter;
import com.splice.service.pdf.PdfAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BatchProcessorTests {
    @TempDir
    Path tempDir;
    Path inputDir;
    Path outputDir;

    private final DocumentAnalyzer mockAnalyzer = mock(PdfAnalyzer.class);
    private final ResultWriter mockWriter = mock(JsonResultWriter.class);
    private final BatchProcessor processor = new BatchProcessor(mockAnalyzer, mockWriter);

    private static final IngestedDocument DUMMY_DOC = mock(IngestedDocument.class);

    @BeforeEach
    void setup() throws IOException {
        inputDir = Files.createDirectory(tempDir.resolve("in"));
        outputDir = Files.createDirectory(tempDir.resolve("out"));

        lenient().when(mockWriter.extension()).thenReturn(".json");
    }

    @Test
    @DisplayName("Should process valid files and output to separate destination")
    void shouldWriteToOutput() throws IOException {
        Path pdf1 = Files.createFile(inputDir.resolve("doc1.pdf"));
        Path pdf2 = Files.createFile(inputDir.resolve("doc2.pdf"));

        when(mockAnalyzer.supports(pdf1)).thenReturn(true);
        when(mockAnalyzer.supports(pdf2)).thenReturn(true);
        when(mockAnalyzer.analyze(any(Path.class))).thenReturn(DUMMY_DOC);

        processor.process(inputDir, outputDir, false);

        verify(mockWriter).write(DUMMY_DOC, outputDir.resolve("doc1.json"));
        verify(mockWriter).write(DUMMY_DOC, outputDir.resolve("doc2.json"));
    }

    @Test
    @DisplayName("Should skip files not supported by analyzer")
    void shouldSkipUnsupported() throws IOException {
        Path pdf = Files.createFile(inputDir.resolve("valid.pdf"));
        Path txt = Files.createFile(inputDir.resolve("notes.txt"));

        when(mockAnalyzer.supports(pdf)).thenReturn(true);
        when(mockAnalyzer.supports(txt)).thenReturn(false);
        when(mockAnalyzer.analyze(pdf)).thenReturn(DUMMY_DOC);

        processor.process(inputDir, outputDir, false);

        verify(mockWriter).write(DUMMY_DOC, outputDir.resolve("valid.json"));
        verify(mockWriter, never()).write(any(), eq(outputDir.resolve("notes.json")));
    }

    @Test
    @DisplayName("Should continue processing next files even if one crashes")
    void shouldNotStopOnError() throws IOException {
        Path bad = Files.createFile(inputDir.resolve("bad.pdf"));
        Path good = Files.createFile(inputDir.resolve("good.pdf"));

        when(mockAnalyzer.supports(any())).thenReturn(true);

        when(mockAnalyzer.analyze(bad)).thenThrow(new IOException("Corrupted"));
        when(mockAnalyzer.analyze(good)).thenReturn(DUMMY_DOC);

        processor.process(inputDir, outputDir, false);

        verify(mockWriter).write(DUMMY_DOC, outputDir.resolve("good.json"));
        verify(mockAnalyzer).analyze(bad);
    }

    @Test
    @DisplayName("Should replicate directory structure in output (Mirroring)")
    void shouldMirrorStructure() throws IOException {
        Path subFolder = Files.createDirectory(inputDir.resolve("sub"));
        Path doc = Files.createFile(subFolder.resolve("doc.pdf"));

        when(mockAnalyzer.supports(doc)).thenReturn(true);
        when(mockAnalyzer.supports(subFolder)).thenReturn(false);
        when(mockAnalyzer.analyze(doc)).thenReturn(DUMMY_DOC);

        processor.process(inputDir, outputDir, true);

        Path expectedOutput = outputDir.resolve("sub").resolve("doc.json");

        ArgumentCaptor<Path> pathCaptor = ArgumentCaptor.forClass(Path.class);
        verify(mockWriter).write(eq(DUMMY_DOC), pathCaptor.capture());

        assertEquals(expectedOutput.toAbsolutePath(), pathCaptor.getValue().toAbsolutePath());
    }
}