package com.luminous.dto.ai;

import java.util.List;

public record ChatResponse(String reply, List<AiProductView> products, DraftResponse draft,
                           boolean fallback, String error) {

    public static ChatResponse ok(String reply, List<AiProductView> products, DraftResponse draft, boolean fallback) {
        return new ChatResponse(reply, products, draft, fallback, null);
    }

    public static ChatResponse error(String error) {
        return new ChatResponse(null, List.of(), null, false, error);
    }
}
