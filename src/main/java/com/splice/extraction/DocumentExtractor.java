package com.splice.extraction;

import com.splice.model.document.IngestedDocument;

import java.io.IOException;
import java.nio.file.Path;

public interface DocumentExtractor {
    IngestedDocument extract(Path path) throws IOException;
    boolean supports(Path path);
}
