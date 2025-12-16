package com.splice.service;

import com.splice.model.PageContent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface DocumentAnalyzer {
    List<PageContent> analyze(Path path) throws IOException;
    boolean supports(Path path);
}
