package com.splice.service.core;

import com.splice.model.PageContent;
import com.splice.model.TextContent;
import com.splice.service.DocumentAnalyzer;
import com.splice.service.ResultWriter;
import com.splice.service.json.JsonResultWriter;
import com.splice.service.pdf.PdfAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.Mockito.*;

public class BatchProcessorTests {
    @TempDir
    Path tempDir;

    private final DocumentAnalyzer mockAnalyzer = mock(PdfAnalyzer.class);
    private final ResultWriter mockWriter = mock(JsonResultWriter.class);
    private final BatchProcessor processor = new BatchProcessor(mockAnalyzer, mockWriter);

    private static final List<PageContent> DEFAULT_CONTENT = List.of(new TextContent("Default", 1));

    @BeforeEach
    void setup() {
        lenient().when(mockWriter.extension()).thenReturn(".json");
    }

    @Test
    public void process_validPdfOnly_shouldCallWriterWithSpecificJsonPaths() throws IOException {
        Files.createFile(tempDir.resolve("doc1.pdf"));
        Files.createFile(tempDir.resolve("doc2.pdf"));

        when(mockAnalyzer.supports(any(Path.class))).thenReturn(true);
        when(mockAnalyzer.analyze(any(Path.class))).thenReturn(DEFAULT_CONTENT);

        processor.process(tempDir, tempDir, false);

        verify(mockWriter).write(eq(DEFAULT_CONTENT), eq(tempDir.resolve("doc1.json")));
        verify(mockWriter).write(eq(DEFAULT_CONTENT), eq(tempDir.resolve("doc2.json")));
    }

    @Test
    public void process_mixedFiles_shouldIgnoreNonPdf() throws IOException {
        var pdfFile = Files.createFile(tempDir.resolve("doc1.pdf"));
        var txtFile = Files.createFile(tempDir.resolve("notes.txt"));

        when(mockAnalyzer.supports(pdfFile)).thenReturn(true);
        when(mockAnalyzer.supports(txtFile)).thenReturn(false);
        when(mockAnalyzer.analyze(pdfFile)).thenReturn(DEFAULT_CONTENT);

        processor.process(tempDir, tempDir, false);

        verify(mockWriter).write(eq(DEFAULT_CONTENT), eq(tempDir.resolve("doc1.json")));

        verify(mockWriter, never()).write(any(), eq(tempDir.resolve("notes.json")));
    }

    @Test
    public void process_corruptedFile_shouldContinueProcessingOthers() throws IOException {
        var goodPdf = Files.createFile(tempDir.resolve("good.pdf"));
        var badPdf  = Files.createFile(tempDir.resolve("bad_crash.pdf"));

        when(mockAnalyzer.supports(badPdf)).thenReturn(true);
        when(mockAnalyzer.analyze(badPdf)).thenThrow(new IOException("Simulated corrupted file"));

        when(mockAnalyzer.supports(goodPdf)).thenReturn(true);
        when(mockAnalyzer.analyze(goodPdf)).thenReturn(DEFAULT_CONTENT);

        processor.process(tempDir, tempDir, false);

        verify(mockWriter).write(eq(DEFAULT_CONTENT), eq(tempDir.resolve("good.json")));
    }

    @Test
    public void process_recursiveTrue_shouldProcessSubdirectories() throws IOException {
        Files.createFile(tempDir.resolve("root_doc.pdf"));
        Path subfolder = Files.createDirectory(tempDir.resolve("subfolder"));
        Files.createFile(subfolder.resolve("sub_doc.pdf"));

        when(mockAnalyzer.supports(any(Path.class))).thenReturn(true);
        when(mockAnalyzer.analyze(any(Path.class))).thenReturn(DEFAULT_CONTENT);

        processor.process(tempDir, tempDir, true);

        Path expectedRootOutput = tempDir.resolve("root_doc.json");

        Path expectedSubOutput = tempDir.resolve("sub_doc.json");

        verify(mockWriter).write(eq(DEFAULT_CONTENT), eq(expectedRootOutput));
        verify(mockWriter).write(eq(DEFAULT_CONTENT), eq(expectedSubOutput));
    }

    @Test
    public void process_recursiveFalse_shouldIgnoreSubdirectories() throws IOException {
        Files.createFile(tempDir.resolve("root_doc.pdf"));
        Path subfolder = Files.createDirectory(tempDir.resolve("subfolder"));
        Files.createFile(subfolder.resolve("sub_doc.pdf"));

        when(mockAnalyzer.supports(any(Path.class))).thenReturn(true);
        when(mockAnalyzer.analyze(any(Path.class))).thenReturn(DEFAULT_CONTENT);

        processor.process(tempDir, tempDir, false);

        Path expectedRootOutput = tempDir.resolve("root_doc.json");

        verify(mockWriter).write(eq(DEFAULT_CONTENT), eq(expectedRootOutput));

        verify(mockWriter, never()).write(any(), eq(tempDir.resolve("sub_doc.json")));
    }
}