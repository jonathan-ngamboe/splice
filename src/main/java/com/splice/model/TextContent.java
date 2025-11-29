package com.splice.model;

public record TextContent(String text, int pageNumber) implements PageContent {
}
