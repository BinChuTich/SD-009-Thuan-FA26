package com.example.sd009thuan.dto;

import java.util.List;

public record BulkEmailResponse(
        int totalRequested,
        int totalSent,
        int totalFailed,
        List<String> failedEmails,
        String message
) {}

