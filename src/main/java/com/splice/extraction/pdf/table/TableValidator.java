package com.splice.extraction.pdf.table;

import technology.tabula.Table;

import java.util.*;

public class TableValidator {

    private static final int MIN_EFFECTIVE_ROWS = 2;
    private static final int MIN_EFFECTIVE_COLS = 2;
    private static final int MIN_EFFECTIVE_CELLS = 2;

    private static final double MAX_EFFECTIVE_SPARSITY = 0.90;

    private static final double MAX_AVG_TEXT_LENGTH = 150.0;
    private static final int MAX_SINGLE_CELL_LENGTH = 500;

    public static boolean isNoise(Table table) {
        if (table == null) return true;

        TableTopology topology = analyzeTopology(table);

        if (topology.effectiveRows < MIN_EFFECTIVE_ROWS ||
                topology.effectiveCols < MIN_EFFECTIVE_COLS ||
                topology.filledCellCount < MIN_EFFECTIVE_CELLS) {
            return true;
        }

        TableMetrics metrics = calculateEffectiveMetrics(topology);

        if (metrics.effectiveSparsity > MAX_EFFECTIVE_SPARSITY) return true;
        if (metrics.avgCellLength > MAX_AVG_TEXT_LENGTH) return true;

        return metrics.maxCellLength > MAX_SINGLE_CELL_LENGTH;
    }


    private static TableTopology analyzeTopology(Table table) {
        Set<Integer> nonEmptyRowIndices = new HashSet<>();
        Set<Integer> nonEmptyColIndices = new HashSet<>();
        long totalCharCount = 0;
        int maxLen = 0;
        int filledCells = 0;

        var rows = table.getRows();
        for (int r = 0; r < rows.size(); r++) {
            var row = rows.get(r);
            for (int c = 0; c < row.size(); c++) {
                String text = row.get(c).getText().trim();

                if (!text.isEmpty()) {
                    nonEmptyRowIndices.add(r);
                    nonEmptyColIndices.add(c);

                    filledCells++;
                    int len = text.length();
                    totalCharCount += len;
                    if (len > maxLen) maxLen = len;
                }
            }
        }

        return new TableTopology(
                nonEmptyRowIndices.size(),
                nonEmptyColIndices.size(),
                filledCells,
                totalCharCount,
                maxLen
        );
    }

    private static TableMetrics calculateEffectiveMetrics(TableTopology topo) {
        int theoreticalArea = topo.effectiveRows * topo.effectiveCols;

        if (theoreticalArea == 0) return new TableMetrics(1.0, 0, 0);

        double sparsity = 1.0 - ((double) topo.filledCellCount / theoreticalArea);

        double avgLen = (topo.filledCellCount == 0) ? 0 : (double) topo.totalCharCount / topo.filledCellCount;

        return new TableMetrics(sparsity, avgLen, topo.maxLen());
    }

    private record TableTopology(int effectiveRows, int effectiveCols, int filledCellCount, long totalCharCount, int maxLen) {}
    private record TableMetrics(double effectiveSparsity, double avgCellLength, int maxCellLength) {}
}