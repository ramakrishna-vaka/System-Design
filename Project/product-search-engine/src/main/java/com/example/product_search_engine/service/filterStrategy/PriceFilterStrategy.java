package com.example.product_search_engine.service.filterStrategy;

import com.example.product_search_engine.model.Product;
import com.example.product_search_engine.model.ProductSearchRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class PriceFilterStrategy implements ProductFilterStrategy {

    @Override
    public boolean isApplicable(ProductSearchRequest request) {
        return request.getMinPrice() != null ||
                request.getMaxPrice() != null;
    }

    @Override
    public Specification<Product> build(ProductSearchRequest request) {

        return (root, query, cb) -> {

            if (request.getMinPrice() != null &&
                    request.getMaxPrice() != null) {

                return cb.between(
                        root.get("price"),
                        request.getMinPrice(),
                        request.getMaxPrice()
                );
            }

            if (request.getMinPrice() != null) {
                return cb.greaterThanOrEqualTo(
                        root.get("price"),
                        request.getMinPrice()
                );
            }

            return cb.lessThanOrEqualTo(
                    root.get("price"),
                    request.getMaxPrice()
            );
        };
    }
}
