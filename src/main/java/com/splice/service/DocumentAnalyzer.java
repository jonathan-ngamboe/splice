package com.splice.service;

import com.splice.model.IngestedDocument;

import java.io.IOException;
import java.nio.file.Path;

public interface DocumentAnalyzer {
    IngestedDocument analyze(Path path) throws IOException;
    boolean supports(Path path);
}
