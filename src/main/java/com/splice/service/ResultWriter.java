package com.splice.service;

import com.splice.model.PageContent;

import java.nio.file.Path;

import java.util.List;

public interface ResultWriter {
    public void write(List<PageContent> results, Path destination);
}
