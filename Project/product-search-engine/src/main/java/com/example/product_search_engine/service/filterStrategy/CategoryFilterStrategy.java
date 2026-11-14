package com.example.product_search_engine.service.filterStrategy;

import com.example.product_search_engine.model.Product;
import com.example.product_search_engine.model.ProductSearchRequest;
import com.example.product_search_engine.repository.CategoryRepository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryFilterStrategy implements ProductFilterStrategy {

    private final CategoryRepository categoryRepository;

    public CategoryFilterStrategy(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public boolean isApplicable(ProductSearchRequest request) {
        return request.getCategoryId() != null;
    }

    @Override
    public Specification<Product> build(ProductSearchRequest request) {

        List<Long> categoryIds =
                categoryRepository.findCategoryAndDescendantIds(
                        request.getCategoryId()
                );

        return (root, query, cb) -> {

            if (categoryIds.isEmpty()) {
                return cb.disjunction();
            }

            return root.get("category")
                    .get("id")
                    .in(categoryIds);
        };
    }
}