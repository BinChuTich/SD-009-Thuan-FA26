package com.example.sd009thuan.dto;

import java.util.Collections;
import java.util.List;

public record ExcelImportResult(
        int totalRows,
        int successCount,
        int errorCount,
        List<String> errors,
        String message,
        List<String> duplicateProducts
) {
    public ExcelImportResult(int totalRows, int successCount, int errorCount, List<String> errors, String message) {
        this(totalRows, successCount, errorCount, errors, message, Collections.emptyList());
    }
}

