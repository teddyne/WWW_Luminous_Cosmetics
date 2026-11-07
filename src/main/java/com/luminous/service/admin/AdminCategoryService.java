package com.luminous.service.admin;

import com.luminous.dto.CategoryForm;
import com.luminous.dto.CategoryRow;
import com.luminous.model.Category;
import com.luminous.model.enums.CategoryStatus;
import com.luminous.repository.category.CategoryRepository;
import com.luminous.repository.product.ProductRepository;
import com.luminous.service.FileStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminCategoryService {

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final FileStorageService files;

    public AdminCategoryService(CategoryRepository categories, ProductRepository products,
                                FileStorageService files) {
        this.categories = categories;
        this.products = products;
        this.files = files;
    }

    public Page<CategoryRow> page(String q, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Category> result = categories.findAll(spec(q, status), pageable);
        return result.map(this::toRow);
    }

    private Specification<Category> spec(String q, String status) {
        return (root, query, cb) -> {
            List<Predicate> and = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                and.add(cb.like(cb.lower(root.get("name")), "%" + q.trim().toLowerCase() + "%"));
            }
            CategoryStatus filter = CategoryStatus.from(status);
            if (filter != null) {
                and.add(cb.equal(root.get("status"), filter));
            }
            return and.isEmpty() ? null : cb.and(and.toArray(Predicate[]::new));
        };
    }

    @Transactional
    public int[] bulkDelete(List<Integer> ids) {
        int deleted = 0;
        int skipped = 0;
        for (Integer id : ids) {
            try {
                delete(id);
                deleted++;
            } catch (IllegalArgumentException e) {
                skipped++;
            }
        }
        return new int[]{deleted, skipped};
    }

    public List<Category> allActive() {
        return categories.findByStatusOrderByIdAsc(CategoryStatus.ACTIVE);
    }

    public CategoryForm formFor(Integer id) {
        Category category = categories.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại sản phẩm"));
        CategoryForm form = new CategoryForm();
        form.setId(category.getId());
        form.setName(category.getName());
        form.setDescription(category.getDescription());
        form.setStatus(category.getStatus().name());
        return form;
    }

    @Transactional
    public void create(CategoryForm form) {
        String name = form.getName().trim();
        if (categories.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Tên loại sản phẩm đã tồn tại");
        }
        Category category = new Category();
        category.setName(name);
        category.setDescription(blankToNull(form.getDescription()));
        category.setStatus(statusOrDefault(form.getStatus()));
        categories.save(category);
        files.ensureCategoryFolder(name);
    }

    @Transactional
    public void update(Integer id, CategoryForm form) {
        Category category = categories.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại sản phẩm"));
        String name = form.getName().trim();
        if (!category.getName().equalsIgnoreCase(name) && categories.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Tên loại sản phẩm đã tồn tại");
        }
        category.setName(name);
        category.setDescription(blankToNull(form.getDescription()));
        category.setStatus(statusOrDefault(form.getStatus()));
        categories.save(category);
        files.ensureCategoryFolder(name);
    }

    @Transactional
    public void toggleStatus(Integer id) {
        Category category = categories.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại sản phẩm"));
        category.setStatus(category.getStatus() == CategoryStatus.ACTIVE
                ? CategoryStatus.INACTIVE : CategoryStatus.ACTIVE);
        categories.save(category);
    }

    @Transactional
    public void delete(Integer id) {
        if (products.countByCategoryId(id) > 0) {
            throw new IllegalArgumentException("Loại sản phẩm còn sản phẩm nên không thể xóa");
        }
        categories.deleteById(id);
    }

    private CategoryRow toRow(Category category) {
        return new CategoryRow(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getStatus(),
                products.countByCategoryId(category.getId()),
                category.getCreatedAt());
    }

    private static CategoryStatus statusOrDefault(String code) {
        CategoryStatus status = CategoryStatus.from(code);
        return status == null ? CategoryStatus.ACTIVE : status;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
