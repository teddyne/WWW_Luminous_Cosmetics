package com.luminous.controller.admin;

import org.springframework.web.util.UriComponentsBuilder;

public final class AdminPageLinks {

    private AdminPageLinks() {
    }

    public static String query(Object... pairs) {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            Object value = pairs[i + 1];
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (text.isEmpty()) {
                continue;
            }
            builder.queryParam(String.valueOf(pairs[i]), text);
        }
        String built = builder.build().encode().toUriString();
        return built.startsWith("?") ? built.substring(1) : built;
    }
}
