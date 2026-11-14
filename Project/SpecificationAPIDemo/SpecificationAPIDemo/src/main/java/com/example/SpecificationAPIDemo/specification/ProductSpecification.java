package com.example.SpecificationAPIDemo.specification;

import com.example.SpecificationAPIDemo.model.Product;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

    public static Specification<Product> hasCategory(String category) {

        return (root, query, cb) ->
                cb.equal(root.get("category"), category);
    }

    public static Specification<Product> hasBrand(String brand) {

        return (root, query, cb) ->
                cb.equal(root.get("brand"), brand);
    }

    public static Specification<Product> hasName(String name) {

        return (root, query, cb) ->
                cb.like(
                        cb.lower(root.get("name")),
                        "%" + name.toLowerCase() + "%"
                );
    }

    public static Specification<Product> hasPrice(Integer price) {

        return (root, query, cb) ->
                cb.equal(root.get("price"), price);
    }
}