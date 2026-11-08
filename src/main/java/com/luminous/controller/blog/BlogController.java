package com.luminous.controller.blog;

import com.luminous.model.Article;
import com.luminous.repository.blog.ArticleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;


@Controller
public class BlogController {

    public record BlogCategory(String code, String label) {
    }

    public static final List<BlogCategory> CATEGORIES = List.of(
            new BlogCategory(Article.CAT_SKINCARE, "Chăm sóc da"),
            new BlogCategory(Article.CAT_SUNSCREEN, "Chống nắng"),
            new BlogCategory(Article.CAT_GREEN, "Sống xanh"),
            new BlogCategory(Article.CAT_PRODUCT, "Sản phẩm"));

    private final ArticleRepository articles;

    public BlogController(ArticleRepository articles) {
        this.articles = articles;
    }

    @GetMapping("/blog")
    public String list(@RequestParam(required = false) String category,
                       @RequestParam(required = false) String q,
                       Model model) {
        List<Article> all = (category == null || category.isBlank() || "all".equals(category))
                ? articles.findByIsPublishedTrueOrderByPublishedAtDesc()
                : articles.findByIsPublishedTrueAndCategoryOrderByPublishedAtDesc(category.trim());
        if (q != null && !q.isBlank()) {
            String keyword = q.trim().toLowerCase();
            all = all.stream()
                    .filter(a -> (a.getTitle() != null && a.getTitle().toLowerCase().contains(keyword))
                            || (a.getExcerpt() != null && a.getExcerpt().toLowerCase().contains(keyword)))
                    .toList();
        }
        model.addAttribute("articles", all);
        model.addAttribute("categories", CATEGORIES);
        model.addAttribute("selectedCategory", category == null ? "all" : category);
        model.addAttribute("q", q);
        return "blog/blog";
    }

    @GetMapping("/blog/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        Article article = articles.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<Article> related = articles.findByIsPublishedTrueAndCategoryOrderByPublishedAtDesc(article.getCategory())
                .stream()
                .filter(a -> !a.getId().equals(article.getId()))
                .limit(3)
                .toList();
        model.addAttribute("article", article);
        model.addAttribute("relatedArticles", related);
        return "blog/detail";
    }
}
