package com.splice.service.json;

import com.splice.geometry.BoundingBox;
import com.splice.model.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonResultWriterTests {

    @TempDir
    Path tempDir;

    private final JsonResultWriter writer = new JsonResultWriter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should create a valid JSON file on disk")
    void shouldCreateJsonFile() throws IOException {
        Path destination = tempDir.resolve("output.json");
        IngestedDocument document = createDummyDocument("Simple text");

        writer.write(document, destination);

        assertTrue(Files.exists(destination), "File should be created");
        assertTrue(Files.size(destination) > 0, "File should not be empty");
    }

    @Test
    @DisplayName("Should serialize Metadata correctly")
    void shouldSerializeMetadata() throws IOException {
        Path destination = tempDir.resolve("metadata.json");
        IngestedDocument document = createDummyDocument("Content");

        writer.write(document, destination);

        JsonNode rootNode = objectMapper.readTree(destination.toFile());
        JsonNode metadata = rootNode.get("metadata");

        assertEquals("test_file.pdf", metadata.get("filename").asString());
        assertEquals("hash123", metadata.get("fileHash").asString());
        assertEquals(5, metadata.get("totalPages").asInt());
    }

    @Test
    @DisplayName("Should handle special characters properly (JSON Escaping)")
    void shouldHandleSpecialCharacters() {
        String trickyText = "Line 1\nLine 2 contains \"quotes\" and \t tabs.";
        Path destination = tempDir.resolve("special_chars.json");
        IngestedDocument document = createDummyDocument(trickyText);

        writer.write(document, destination);

        JsonNode rootNode = objectMapper.readTree(destination.toFile());
        String savedText = rootNode.get("elements").get(0).get("content").get("text").asString();

        assertEquals(trickyText, savedText, "JSON logic should preserve special characters");
    }

    @Test
    @DisplayName("Should verify structure of DocumentElements")
    void shouldSerializeElementsStructure() {
        Path destination = tempDir.resolve("structure.json");
        IngestedDocument document = createDummyDocument("Val");

        writer.write(document, destination);

        JsonNode root = objectMapper.readTree(destination.toFile());
        JsonNode element = root.get("elements").get(0);

        assertEquals("elem-1", element.get("id").asString());
        assertEquals("NARRATIVE_TEXT", element.get("type").asString());

        assertEquals(1, element.get("location").get("pageNumber").asInt());
    }

    private IngestedDocument createDummyDocument(String textContent) {
        var metadata = new DocumentMetadata("test_file.pdf", "hash123", 5, 1200L);
        var location = new Location(1, new BoundingBox(0, 0, 100, 100));
        var content = new TextContent(textContent);

        var element = new DocumentElement(
                "elem-1",
                ElementType.NARRATIVE_TEXT,
                location,
                null,
                content
        );

        return new IngestedDocument("doc-id-1", metadata, List.of(element));
    }
}