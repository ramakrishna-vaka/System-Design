package com.example.product_search_engine.service.filterStrategy;

import com.example.product_search_engine.model.Product;
import com.example.product_search_engine.model.ProductSearchRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class SearchFilterStrategy implements ProductFilterStrategy {

    @Override
    public boolean isApplicable(ProductSearchRequest request) {
        return request.getKeyword() != null
                && !request.getKeyword().isBlank();
    }

    @Override
    public Specification<Product> build(ProductSearchRequest request) {
        return (root, query, cb) -> {
            String keyword = "%" +
                    request.getKeyword().toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("name")), keyword),
                    cb.like(cb.lower(root.get("description")), keyword)
            );
        };
    }
}