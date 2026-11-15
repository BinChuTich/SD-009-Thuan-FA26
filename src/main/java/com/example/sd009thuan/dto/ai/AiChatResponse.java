package com.example.sd009thuan.dto.ai;

import java.util.List;

public record AiChatResponse(
        String reply,
        List<AiProductCard> products,
        boolean aiPowered
) {}
