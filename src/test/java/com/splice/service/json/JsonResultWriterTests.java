package com.splice.service.json;

import com.splice.model.TextContent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JsonResultWriterTests {

    @TempDir
    Path tempDir;

    private final JsonResultWriter writer = new JsonResultWriter();

    @Test
    public void write_validContent_shouldCreateFileOnDisk() {
        Path destination = tempDir.resolve("output_existence.json");
        var content = new TextContent("Test", 1);

        writer.write(List.of(content), destination);

        assertTrue(Files.exists(destination), "The output file should be created");
    }

    @Test
    public void write_singlePage_shouldContainCorrectJsonFields() throws IOException {
        Path destination = tempDir.resolve("output_content.json");
        var content = new TextContent("Hello World", 42);

        writer.write(List.of(content), destination);

        String jsonResult = Files.readString(destination);

        assertTrue(jsonResult.contains("\"type\""), "Should contain type key");
        assertTrue(jsonResult.contains("\"TEXT\""), "Should contain type value");

        assertTrue(jsonResult.contains("\"text\""), "Should contain text key");
        assertTrue(jsonResult.contains("Hello World"), "Should contain text value");

        assertTrue(jsonResult.contains("\"pageNumber\""), "Should contain pageNumber key");
        assertTrue(jsonResult.contains("42"), "Should contain the page number value (int)");
    }

    @Test
    public void write_directoryPath_shouldCreateTimestampedFileInside() throws IOException {
        Path directory = tempDir;

        writer.write(List.of(new TextContent("Test", 1)), directory);

        try (var files = Files.list(directory)) {
            List<Path> fileList = files.toList();

            assertEquals(1, fileList.size(), "Should have created exactly one file");

            Path createdFile = fileList.get(0);
            String filename = createdFile.getFileName().toString();

            assertTrue(filename.startsWith("splice_report_"), "File should start with prefix");
            assertTrue(filename.endsWith(".json"), "File should be a JSON");

            assertTrue(Files.size(createdFile) > 0, "File should not be empty");
        }
    }
}