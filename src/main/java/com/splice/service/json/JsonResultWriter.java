package com.splice.service.json;

import com.splice.model.PageContent;
import com.splice.service.ResultWriter;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.type.TypeReference;

import java.nio.file.Files;
import java.nio.file.Path;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.List;

public class JsonResultWriter implements ResultWriter {
    private static final String DEFAULT_NAME = "splice_report";
    private static final String FILE_EXTENSION = ".json";
    private final ObjectMapper mapper;

    public JsonResultWriter() {
        this.mapper = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }

    @Override
    public void write(List<PageContent> content, Path destination) {
        Path finalFile = destination;

        if(Files.isDirectory(destination)) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = DEFAULT_NAME + "_" + timestamp + FILE_EXTENSION;
            finalFile = destination.resolve(filename);
        }

        mapper.writerFor(new TypeReference<List<PageContent>>() {})
                .writeValue(finalFile.toFile(), content);
    }

    @Override
    public String extension() {
        return FILE_EXTENSION;
    }

}
