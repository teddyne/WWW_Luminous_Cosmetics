package com.luminous.repository.blog;

import com.luminous.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Integer> {

    List<Article> findByIsPublishedTrueOrderByPublishedAtDesc();

    List<Article> findByIsPublishedTrueAndCategoryOrderByPublishedAtDesc(String category);

    Optional<Article> findBySlugAndIsPublishedTrue(String slug);

    List<Article> findByIsPublishedTrueAndCategoryNotOrderByPublishedAtDesc(String category);
}
