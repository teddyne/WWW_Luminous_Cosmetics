package com.luminous.service.ai;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class AiPolicyDocuments {

    public record PolicyDoc(String file, String title, String content) {
    }

    private static final Logger log = LoggerFactory.getLogger(AiPolicyDocuments.class);

    private static final List<String> FILES = List.of(
            "ai-documents/01-van-chuyen.txt",
            "ai-documents/02-thanh-toan.txt",
            "ai-documents/03-doi-tra.txt",
            "ai-documents/04-diem-thuong-voucher.txt",
            "ai-documents/05-tai-khoan-don-hang.txt",
            "ai-documents/06-mua-tai-cua-hang.txt"
    );

    private final List<PolicyDoc> docs = new ArrayList<>();

    @PostConstruct
    public void load() {
        for (String file : FILES) {
            try {
                ClassPathResource resource = new ClassPathResource(file);
                String content = resource.getContentAsString(StandardCharsets.UTF_8).trim();
                if (content.isBlank()) {
                    continue;
                }
                String title = content.lines().findFirst().orElse(file);
                docs.add(new PolicyDoc(file, title, content));
            } catch (Exception e) {
                log.warn("Không đọc được tài liệu chính sách {}: {}", file, e.getMessage());;
            }
        }
        log.info("Đã nạp {} tài liệu chính sách cho trợ lý AI", docs.size());
    }

    public List<PolicyDoc> all() {
        return List.copyOf(docs);
    }

    public List<PolicyDoc> search(String query, int limit) {
        Set<String> queryTokens = AiTextUtils.tokenSet(query);
        if (queryTokens.isEmpty()) {
            return List.of();
        }
        return docs.stream()
                .map(d -> new ScoredDoc(d, score(d, queryTokens)))
                .filter(s -> s.score > 0)
                .sorted(Comparator.comparingInt(ScoredDoc::score).reversed())
                .limit(limit <= 0 ? 3 : limit)
                .map(ScoredDoc::doc)
                .toList();
    }

    private int score(PolicyDoc doc, Set<String> queryTokens) {
        Set<String> docTokens = AiTextUtils.tokenSet(doc.content());
        int hits = 0;
        for (String token : queryTokens) {
            if (docTokens.contains(token)) {
                hits++;
            }
        }
        return hits;
    }

    private record ScoredDoc(PolicyDoc doc, int score) {
    }
}
