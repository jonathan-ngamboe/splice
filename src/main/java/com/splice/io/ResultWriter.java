package com.splice.io;

import com.splice.model.document.IngestedDocument;

import java.nio.file.Path;

public interface ResultWriter {
    void write(IngestedDocument results, Path destination);
    String extension();
}
