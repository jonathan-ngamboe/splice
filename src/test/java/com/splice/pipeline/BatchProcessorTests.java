package com.splice.pipeline;

import com.splice.extraction.DocumentExtractor;
import com.splice.extraction.spi.AssetStorage;
import com.splice.extraction.spi.ExtractorProvider;
import com.splice.io.ResultWriter;
import com.splice.model.document.DocumentMetadata;
import com.splice.model.document.IngestedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchProcessorTests {

    @TempDir
    Path tempDir;
    Path inputDir;
    Path outputDir;

    @Mock
    private ExtractorProvider mockProvider;
    @Mock
    private DocumentExtractor mockExtractor;
    @Mock
    private ResultWriter mockWriter;
    @Mock
    private IngestedDocument dummyDoc;
    @Mock
    private DocumentMetadata dummyMetadata;

    private BatchProcessor processor;

    @BeforeEach
    void setup() throws IOException {
        inputDir = Files.createDirectory(tempDir.resolve("in"));
        outputDir = Files.createDirectory(tempDir.resolve("out"));

        processor = new BatchProcessor(mockWriter, List.of(mockProvider));

        lenient().when(mockWriter.extension()).thenReturn(".json");

        lenient().when(dummyDoc.metadata()).thenReturn(dummyMetadata);
        lenient().when(dummyMetadata.totalPages()).thenReturn(1);
    }

    @Test
    @DisplayName("Should process valid files using the provider strategy")
    void shouldWriteToOutput() throws IOException {
        // GIVEN
        Path pdf1 = Files.createFile(inputDir.resolve("doc1.pdf"));
        Path pdf2 = Files.createFile(inputDir.resolve("doc2.pdf"));

        // Simulation: Le provider accepte les fichiers
        when(mockProvider.supports(pdf1)).thenReturn(true);
        when(mockProvider.supports(pdf2)).thenReturn(true);

        when(mockProvider.create(any(AssetStorage.class))).thenReturn(mockExtractor);

        when(mockExtractor.extract(any(Path.class))).thenReturn(dummyDoc);

        processor.process(inputDir, outputDir, false);

        verify(mockWriter).write(dummyDoc, outputDir.resolve("doc1.json"));
        verify(mockWriter).write(dummyDoc, outputDir.resolve("doc2.json"));

        verify(mockProvider, times(2)).create(any(AssetStorage.class));
    }

    @Test
    @DisplayName("Should skip files not supported by any provider")
    void shouldSkipUnsupported() throws IOException {
        Path pdf = Files.createFile(inputDir.resolve("valid.pdf"));
        Path txt = Files.createFile(inputDir.resolve("notes.txt"));

        when(mockProvider.supports(pdf)).thenReturn(true);
        when(mockProvider.supports(txt)).thenReturn(false);

        when(mockProvider.create(any(AssetStorage.class))).thenReturn(mockExtractor);
        when(mockExtractor.extract(pdf)).thenReturn(dummyDoc);

        processor.process(inputDir, outputDir, false);

        verify(mockWriter).write(dummyDoc, outputDir.resolve("valid.json"));
        verify(mockWriter, never()).write(any(), eq(outputDir.resolve("notes.json")));
    }

    @Test
    @DisplayName("Should continue processing next files even if one crashes")
    void shouldNotStopOnError() throws IOException {
        Path bad = Files.createFile(inputDir.resolve("bad.pdf"));
        Path good = Files.createFile(inputDir.resolve("good.pdf"));

        when(mockProvider.supports(any())).thenReturn(true);
        when(mockProvider.create(any(AssetStorage.class))).thenReturn(mockExtractor);

        when(mockExtractor.extract(bad)).thenThrow(new RuntimeException("Parsing failed"));
        when(mockExtractor.extract(good)).thenReturn(dummyDoc);

        processor.process(inputDir, outputDir, false);

        verify(mockWriter).write(dummyDoc, outputDir.resolve("good.json"));
        verify(mockExtractor).extract(bad);
    }

    @Test
    @DisplayName("Should replicate directory structure and pass correct AssetStorage")
    void shouldMirrorStructure() throws IOException {
        Path subFolder = Files.createDirectory(inputDir.resolve("finance"));
        Path doc = Files.createFile(subFolder.resolve("invoice.pdf"));

        when(mockProvider.supports(doc)).thenReturn(true);
        when(mockProvider.supports(subFolder)).thenReturn(false); // Directory check
        when(mockProvider.create(any(AssetStorage.class))).thenReturn(mockExtractor);
        when(mockExtractor.extract(doc)).thenReturn(dummyDoc);

        processor.process(inputDir, outputDir, true);

        Path expectedJsonOutput = outputDir.resolve("finance").resolve("invoice.json");

        ArgumentCaptor<Path> pathCaptor = ArgumentCaptor.forClass(Path.class);
        verify(mockWriter).write(eq(dummyDoc), pathCaptor.capture());
        assertEquals(expectedJsonOutput.toAbsolutePath(), pathCaptor.getValue().toAbsolutePath());
    }
}