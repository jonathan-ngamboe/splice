package com.splice.model;

public record ImageContent(byte[] data, int pageNumber) implements PageContent {
}
