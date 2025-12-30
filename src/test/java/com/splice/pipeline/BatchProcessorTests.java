package com.splice.pipeline;

import com.splice.model.document.IngestedDocument;
import com.splice.extraction.DocumentExtractor;
import com.splice.io.ResultWriter;
import com.splice.io.json.JsonResultWriter;
import com.splice.extraction.pdf.PdfExtractor;
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

    private final DocumentExtractor mockAnalyzer = mock(PdfExtractor.class);
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
        when(mockAnalyzer.extract(any(Path.class))).thenReturn(DUMMY_DOC);

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
        when(mockAnalyzer.extract(pdf)).thenReturn(DUMMY_DOC);

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

        when(mockAnalyzer.extract(bad)).thenThrow(new IOException("Corrupted"));
        when(mockAnalyzer.extract(good)).thenReturn(DUMMY_DOC);

        processor.process(inputDir, outputDir, false);

        verify(mockWriter).write(DUMMY_DOC, outputDir.resolve("good.json"));
        verify(mockAnalyzer).extract(bad);
    }

    @Test
    @DisplayName("Should replicate directory structure in output (Mirroring)")
    void shouldMirrorStructure() throws IOException {
        Path subFolder = Files.createDirectory(inputDir.resolve("sub"));
        Path doc = Files.createFile(subFolder.resolve("doc.pdf"));

        when(mockAnalyzer.supports(doc)).thenReturn(true);
        when(mockAnalyzer.supports(subFolder)).thenReturn(false);
        when(mockAnalyzer.extract(doc)).thenReturn(DUMMY_DOC);

        processor.process(inputDir, outputDir, true);

        Path expectedOutput = outputDir.resolve("sub").resolve("doc.json");

        ArgumentCaptor<Path> pathCaptor = ArgumentCaptor.forClass(Path.class);
        verify(mockWriter).write(eq(DUMMY_DOC), pathCaptor.capture());

        assertEquals(expectedOutput.toAbsolutePath(), pathCaptor.getValue().toAbsolutePath());
    }
}