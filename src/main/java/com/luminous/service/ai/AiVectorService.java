package com.luminous.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luminous.model.AiDocumentVector;
import com.luminous.model.Product;
import com.luminous.model.ProductVariant;
import com.luminous.repository.ai.AiDocumentVectorRepository;
import com.luminous.repository.product.ProductRepository;
import com.luminous.repository.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AiVectorService {

    public record ProductHit(Integer productId, double score) {
    }

    public record PolicyHit(String content, double score) {
    }

    public record ReviewHit(Integer productId, String content, double score) {
    }

    private record StoredVector(String docId, String docType, Integer refId, String content, float[] embedding) {
    }

    private static final Logger log = LoggerFactory.getLogger(AiVectorService.class);
    private static final int EMBED_BATCH_SIZE = 20;

    private final AiDocumentVectorRepository vectorRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final AiPolicyDocuments policies;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.vector.top-k:5}")
    private int topK;

    @Value("${app.ai.vector.threshold:0.5}")
    private double threshold;

    private volatile List<StoredVector> cache = List.of();

    @EventListener(ApplicationReadyEvent.class)
    public void indexOnStartup() {
        loadCacheFromDb();
        log.info("Vector store san sang tu DB: {} tai lieu", cache.size());
        Thread sync = new Thread(() -> {
            try {
                int count = syncIncremental();
                log.info("Vector dong bo xong: {} tai lieu", count);
            } catch (Exception e) {
                log.warn("Khong dong bo duoc vector luc khoi dong ({}), tro ly dung vector cu trong DB", e.getMessage());
            }
        }, "ai-vector-sync");
        sync.setDaemon(true);
        sync.start();
    }

    @Transactional
    public int rebuildAll() {
        EmbeddingModel model = embeddingModelProvider.getIfAvailable();
        if (model == null) {
            loadCacheFromDb();
            return cache.size();
        }
        List<DocInput> docs = collectDocuments();
        List<float[]> embeddings = embedAll(model, docs.stream().map(DocInput::content).toList());
        Set<String> currentIds = new HashSet<>();
        for (int i = 0; i < docs.size(); i++) {
            DocInput doc = docs.get(i);
            currentIds.add(doc.docId());
            AiDocumentVector row = vectorRepository.findByDocId(doc.docId())
                    .orElse(new AiDocumentVector(doc.docId(), doc.docType(), doc.refId(), doc.content(), ""));
            row.setContent(doc.content());
            row.setEmbedding(toJson(embeddings.get(i)));
            vectorRepository.save(row);
        }
        deleteStale(currentIds);
        loadCacheFromDb();
        return cache.size();
    }

    @Transactional
    public int syncIncremental() {
        EmbeddingModel model = embeddingModelProvider.getIfAvailable();
        if (model == null) {
            loadCacheFromDb();
            return cache.size();
        }
        List<DocInput> docs = collectDocuments();
        List<DocInput> changed = new ArrayList<>();
        List<String> changedContents = new ArrayList<>();
        Set<String> currentIds = new HashSet<>();
        int reused = 0;
        for (DocInput doc : docs) {
            currentIds.add(doc.docId());
            var existing = vectorRepository.findByDocId(doc.docId());
            if (existing.isPresent() && java.util.Objects.equals(existing.get().getContent(), doc.content())
                    && parseEmbedding(existing.get().getEmbedding()) != null) {
                reused++;
                continue;
            }
            changed.add(doc);
            changedContents.add(doc.content());
        }
        if (!changed.isEmpty()) {
            List<float[]> embeddings = embedAll(model, changedContents);
            for (int i = 0; i < changed.size(); i++) {
                DocInput doc = changed.get(i);
                AiDocumentVector row = vectorRepository.findByDocId(doc.docId())
                        .orElse(new AiDocumentVector(doc.docId(), doc.docType(), doc.refId(), doc.content(), ""));
                row.setContent(doc.content());
                row.setEmbedding(toJson(embeddings.get(i)));
                vectorRepository.save(row);
            }
        }
        deleteStale(currentIds);
        loadCacheFromDb();
        log.info("Vector dong bo tang dan: {} tai lieu, dung lai {} vector cu, {} tai lieu moi/doi",
                docs.size(), reused, changed.size());
        return cache.size();
    }

    private record DocInput(String docId, String docType, Integer refId, String content) {
    }

    private List<DocInput> collectDocuments() {
        List<DocInput> docs = new ArrayList<>();
        for (Product product : productRepository.findByIsActiveTrueOrderByCreatedAtDesc()) {
            docs.add(new DocInput("product:" + product.getId(), "PRODUCT", product.getId(), productText(product)));
        }
        for (AiPolicyDocuments.PolicyDoc policy : policies.all()) {
            docs.add(new DocInput("policy:" + policy.file(), "POLICY", null, policy.content()));
        }
        for (Product product : productRepository.findByIsActiveTrueOrderByCreatedAtDesc()) {
            String reviewText = reviewText(product.getId());
            if (reviewText != null) {
                docs.add(new DocInput("review:" + product.getId(), "REVIEW", product.getId(), reviewText));
            }
        }
        return docs;
    }

    private void deleteStale(Set<String> currentIds) {
        for (AiDocumentVector row : vectorRepository.findAll()) {
            if (!currentIds.contains(row.getDocId())) {
                vectorRepository.delete(row);
            }
        }
    }

    public List<ProductHit> searchProducts(String query, int limit) {
        float[] queryVector = embedQuery(query);
        if (queryVector == null) {
            return List.of();
        }
        return cache.stream()
                .filter(v -> "PRODUCT".equals(v.docType()))
                .map(v -> new ProductHit(v.refId(), AiTextUtils.cosine(queryVector, v.embedding())))
                .filter(h -> h.score() >= threshold)
                .sorted(Comparator.comparingDouble(ProductHit::score).reversed())
                .limit(limit <= 0 ? topK : limit)
                .toList();
    }

    public List<PolicyHit> searchPolicies(String query, int limit) {
        float[] queryVector = embedQuery(query);
        if (queryVector == null) {
            return List.of();
        }
        return cache.stream()
                .filter(v -> "POLICY".equals(v.docType()))
                .map(v -> new PolicyHit(v.content(), AiTextUtils.cosine(queryVector, v.embedding())))
                .filter(h -> h.score() >= threshold)
                .sorted(Comparator.comparingDouble(PolicyHit::score).reversed())
                .limit(limit <= 0 ? topK : limit)
                .toList();
    }

    public List<ReviewHit> searchReviews(String query, int limit) {
        float[] queryVector = embedQuery(query);
        if (queryVector == null) {
            return List.of();
        }
        return cache.stream()
                .filter(v -> "REVIEW".equals(v.docType()))
                .map(v -> new ReviewHit(v.refId(), v.content(), AiTextUtils.cosine(queryVector, v.embedding())))
                .filter(h -> h.score() >= threshold)
                .sorted(Comparator.comparingDouble(ReviewHit::score).reversed())
                .limit(limit <= 0 ? topK : limit)
                .toList();
    }

    public boolean isReady() {
        return !cache.isEmpty();
    }

    public int indexedCount() {
        return cache.size();
    }

    public String productText(Product product) {
        StringBuilder text = new StringBuilder();
        text.append(product.getName() == null ? "" : product.getName()).append(" ");
        text.append(product.getBrand() == null ? "" : product.getBrand()).append(" ");
        if (product.getCategory() != null) {
            text.append(product.getCategory().getName()).append(" ");
        }
        text.append(product.getDescription() == null ? "" : product.getDescription()).append(" ");
        text.append(product.getUsageInstructions() == null ? "" : product.getUsageInstructions()).append(" ");
        for (ProductVariant variant : product.getVariants()) {
            if (!Boolean.TRUE.equals(variant.getIsActive())) {
                continue;
            }
            text.append(variant.getSize() == null ? "" : variant.getSize()).append(" ");
            text.append(variant.getIngredients() == null ? "" : variant.getIngredients()).append(" ");
            text.append(variant.getSuitableSkinType() == null ? "" : variant.getSuitableSkinType()).append(" ");
        }
        return text.toString().trim();
    }

    private String reviewText(Integer productId) {
        try {
            Double average = reviewRepository.averageRating(productId);
            long count = reviewRepository.reviewCount(productId);
            if (count == 0) {
                return null;
            }
            List<com.luminous.model.Review> reviews = reviewRepository
                    .findVisibleByProduct(productId, org.springframework.data.domain.PageRequest.of(0, 5))
                    .getContent();
            StringBuilder text = new StringBuilder();
            text.append("Đánh giá sản phẩm id ").append(productId).append(": trung bình ")
                    .append(average == null ? "?" : String.format("%.1f", average))
                    .append(" sao tren ").append(count).append(" danh gia. ");
            for (com.luminous.model.Review review : reviews) {
                if (review.getComment() != null && !review.getComment().isBlank()) {
                    text.append("Khách đánh giá ").append(review.getRating()).append(" sao: ")
                            .append(review.getComment()).append(" ");
                }
            }
            return text.toString().trim();
        } catch (Exception e) {
            log.warn("Khong tao duoc tai lieu review cho san pham {}: {}", productId, e.getMessage());
            return null;
        }
    }

    private List<float[]> embedAll(EmbeddingModel model, List<String> texts) {
        List<float[]> out = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += EMBED_BATCH_SIZE) {
            out.addAll(model.embed(texts.subList(i, Math.min(i + EMBED_BATCH_SIZE, texts.size()))));
        }
        return out;
    }

    private float[] embedQuery(String query) {
        EmbeddingModel model = embeddingModelProvider.getIfAvailable();
        if (model == null || query == null || query.isBlank()) {
            return null;
        }
        try {
            return model.embed(query);
        } catch (Exception e) {
            log.warn("Khong nhung duoc cau hoi ({}), dung tim theo tu khoa", e.getMessage());
            return null;
        }
    }

    private void loadCacheFromDb() {
        List<StoredVector> loaded = new ArrayList<>();
        for (AiDocumentVector row : vectorRepository.findAll()) {
            float[] embedding = parseEmbedding(row.getEmbedding());
            if (embedding != null) {
                loaded.add(new StoredVector(row.getDocId(), row.getDocType(), row.getRefId(), row.getContent(), embedding));
            }
        }
        cache = List.copyOf(loaded);
    }

    private String toJson(float[] embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (Exception e) {
            throw new IllegalStateException("Không ghi được vector", e);
        }
    }

    private float[] parseEmbedding(String json) {
        try {
            return objectMapper.readValue(json, float[].class);
        } catch (Exception e) {
            log.warn("Bo qua vector loi: {}", e.getMessage());
            return null;
        }
    }
}
