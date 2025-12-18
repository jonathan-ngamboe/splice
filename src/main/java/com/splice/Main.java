package com.splice;

import picocli.CommandLine;
import picocli.CommandLine.*;

import java.nio.file.Path;

import java.util.concurrent.Callable;

@Command(name = "splice",
        mixinStandardHelpOptions = true,
        version = "splice 1.0",
        description = "Smart ingestion engine for RAG: Optimizes costs by routing PDF pages to local CPU or cloud OCR based on visual complexity.")
public class Main implements Callable<Integer> {

    @Option(names = {"-i", "--input"},
            description = "Path to a single PDF file or a directory containing PDFs.",
            required = true)
    private Path input;

    @Option(names = {"-o", "--output"},
            description = "Directory where the JSON report and extracted images will be saved.",
            required = true)
    private Path outputDir;

    @Option(names = {"-r", "--recursive"},
            description = "Process subdirectories recursively if input is a directory.")
    private boolean recursive = false;

    @Override
    public Integer call() throws Exception {
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
