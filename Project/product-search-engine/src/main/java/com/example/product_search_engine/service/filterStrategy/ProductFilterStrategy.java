package com.example.product_search_engine.service.filterStrategy;

import com.example.product_search_engine.model.Product;
import com.example.product_search_engine.model.ProductSearchRequest;
import org.springframework.data.jpa.domain.Specification;

public interface ProductFilterStrategy {

    boolean isApplicable(ProductSearchRequest request);

    Specification<Product> build(ProductSearchRequest request);
}
