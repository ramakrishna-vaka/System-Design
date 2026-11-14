package com.example.product_search_engine.service.filterStrategy;

import com.example.product_search_engine.model.Product;
import com.example.product_search_engine.model.ProductAttribute;
import com.example.product_search_engine.model.ProductSearchRequest;

import jakarta.persistence.criteria.Subquery;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class ColorFilterStrategy implements ProductFilterStrategy {

    @Override
    public boolean isApplicable(ProductSearchRequest request) {
        return request.getColors() != null &&
                !request.getColors().isEmpty();
    }

    @Override
    public Specification<Product> build(ProductSearchRequest request) {

        return (root, query, cb) -> {

            Subquery<Long> subquery = query.subquery(Long.class);

            var attribute = subquery.from(ProductAttribute.class);

            subquery.select(attribute.get("product").get("id"));

            subquery.where(
                    cb.equal(attribute.get("product").get("id"), root.get("id")),
                    cb.equal(attribute.get("attributeName"), "color"),
                    attribute.get("attributeValue").in(request.getColors())
            );

            return cb.exists(subquery);
        };
    }
}