package com.splice.service.json;

import com.splice.model.PageContent;
import com.splice.service.ResultWriter;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;

import java.util.List;

public class JsonResultWriter implements ResultWriter {
    private final ObjectMapper mapper;

    public JsonResultWriter() {
        this.mapper = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }

    @Override
    public void write(List<PageContent> results, Path destination) {
        mapper.writeValue(destination.toFile(), results);
    }
}
