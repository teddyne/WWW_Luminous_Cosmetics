package com.luminous.service.ai;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class AiTextUtils {

    private AiTextUtils() {
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String stripped = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'D');
        return stripped.toLowerCase().replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    public static List<String> tokens(String text) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return List.of();
        }
        return Arrays.asList(normalized.split(" "));
    }

    public static Set<String> tokenSet(String text) {
        return tokens(text).stream().filter(t -> t.length() > 1).collect(Collectors.toSet());
    }

    public static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return 0.0;
        }
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
