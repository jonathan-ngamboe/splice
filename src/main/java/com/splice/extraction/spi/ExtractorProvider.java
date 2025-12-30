package com.splice.extraction.spi;

import com.splice.extraction.DocumentExtractor;
import java.nio.file.Path;

public interface ExtractorProvider {
    boolean supports(Path path);

    DocumentExtractor create(AssetStorage storage);
}