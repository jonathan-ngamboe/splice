package com.splice.extraction.spi;

import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.io.IOException;

public interface AssetStorage {
    /**
     * Persist an asset and returns its reference (path, url, id).
     * @param image The PDF image object
     * @param contextPrefix A prefix for organization (e.g., "page_1")
     * @return The URI/Path string to be stored in the Document Model
     */
    String store(PDImageXObject image, String contextPrefix) throws IOException;
}