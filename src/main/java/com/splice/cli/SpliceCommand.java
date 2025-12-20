package com.splice.cli;

import com.splice.service.core.BatchProcessor;
import com.splice.service.json.JsonResultWriter;
import com.splice.service.pdf.PdfAnalyzer;

import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

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
        var analyzer = new PdfAnalyzer();
        var processor = new BatchProcessor(analyzer);
        var writer = new JsonResultWriter();

        var content = processor.ingestDirectory(input, recursive);

        if (!Files.exists(output)) {
            Files.createDirectories(output);
        }
        writer.write(content, output);

        return 0;
    }
}
