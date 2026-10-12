package com.example.sd009thuan.dto;

import java.util.List;

public record ExcelImportResult(
        int totalRows,
        int successCount,
        int errorCount,
        List<String> errors,
        String message
) {}

