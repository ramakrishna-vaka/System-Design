package com.example.product_search_engine.service.filterStrategy;

import com.example.product_search_engine.model.Product;
import com.example.product_search_engine.model.ProductSearchRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class BrandFilterStrategy implements ProductFilterStrategy {

    @Override
    public boolean isApplicable(ProductSearchRequest request) {
        return request.getBrands() != null &&
                !request.getBrands().isEmpty();
    }

    @Override
    public Specification<Product> build(ProductSearchRequest request) {

        return (root, query, cb) ->
                root.get("brand").in(request.getBrands());
    }
}