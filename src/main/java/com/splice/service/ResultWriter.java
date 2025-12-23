package com.splice.service;

import com.splice.model.IngestedDocument;

import java.nio.file.Path;

public interface ResultWriter {
    void write(IngestedDocument results, Path destination);
    String extension();
}
