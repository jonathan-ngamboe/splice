package com.splice.cli;

import com.splice.pipeline.BatchProcessor;
import com.splice.io.json.JsonResultWriter;

import picocli.CommandLine;

import java.nio.file.Path;

import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "splice",
        mixinStandardHelpOptions = true,
        version = "splice 1.0",
        description = "Smart ingestion engine for RAG: Optimizes costs by routing documents to local CPU or cloud OCR based on visual complexity.")
public class SpliceCommand implements Callable<Integer> {
    @CommandLine.Option(names = {"-i", "--input"},
            description = "Path to a single file or a directory containing multiple files.",
            required = true)
    private Path input;

    @CommandLine.Option(names = {"-o", "--output"},
            description = "File or directory where the report and extracted images will be saved.",
            required = true)
    private Path output;

    @CommandLine.Option(names = {"-r", "--recursive"},
            description = "Process subdirectories recursively if input is a directory.")
    private boolean recursive = false;

    @Override
    public Integer call() throws Exception {
        var writer = new JsonResultWriter();
        var providers = List.of(
                com.splice.extraction.pdf.PdfExtractor.PROVIDER
        );
        var processor = new BatchProcessor(writer, providers);

        processor.process(input, output, recursive);

        return 0;
    }
}
