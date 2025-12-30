package com.splice.io.json;

import com.splice.model.document.IngestedDocument;
import com.splice.io.ResultWriter;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.type.TypeReference;

import java.nio.file.Path;

public class JsonResultWriter implements ResultWriter {
    private static final String FILE_EXTENSION = ".json";
    private final ObjectMapper mapper;

    public JsonResultWriter() {
        this.mapper = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }

    @Override
    public void write(IngestedDocument content, Path destinationFile) {
        mapper.writerFor(new TypeReference<IngestedDocument>() {})
                .writeValue(destinationFile.toFile(), content);
    }

    @Override
    public String extension() {
        return FILE_EXTENSION;
    }

}
