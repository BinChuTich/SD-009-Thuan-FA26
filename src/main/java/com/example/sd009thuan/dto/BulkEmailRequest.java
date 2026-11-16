package com.example.sd009thuan.dto;

import java.util.List;

public record BulkEmailRequest(
        List<Long> employeeIds,
        String subject,
        String content,
        String emailType
) {}

