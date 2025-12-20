package com.splice;

import com.splice.cli.SpliceCommand;

import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new SpliceCommand()).execute(args);
        System.exit(exitCode);
    }
}
