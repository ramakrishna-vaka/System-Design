package com.example.SpecificationAPIDemo.controller;

import com.example.SpecificationAPIDemo.model.Product;
import com.example.SpecificationAPIDemo.model.ProductDTO;
import com.example.SpecificationAPIDemo.service.ProductService;
import com.example.SpecificationAPIDemo.specification.ProductSpecification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService){
        this.productService = productService;
    }

    @GetMapping("/search")
    public List<Product> getSpecificProducts(
            @ModelAttribute ProductDTO request) {

        List<Product> allProducts = productService.getAllProducts();

        return allProducts.stream()
                .filter(p -> request.getCategory() == null ||
                        p.getCategory().equalsIgnoreCase(request.getCategory()))
                .filter(p -> request.getBrand() == null ||
                        p.getBrand().equalsIgnoreCase(request.getBrand()))
                .filter(p -> request.getName() == null ||
                        p.getName().toLowerCase()
                                .contains(request.getName().toLowerCase()))
                .filter(p -> request.getPrice() == 0 ||
                        p.getPrice() == request.getPrice())
                .toList();
    }

    @GetMapping("/search/specification")
    public List<Product> getSpecificProductsWithSpecification(
            @ModelAttribute ProductDTO request) {

        Specification<Product> spec =    (root, query, cb) -> cb.conjunction();

        if (request.getCategory() != null) {
            spec = spec.and(
                    ProductSpecification.hasCategory(request.getCategory())
            );
        }

        if (request.getBrand() != null) {
            spec = spec.and(
                    ProductSpecification.hasBrand(request.getBrand())
            );
        }

        if (request.getName() != null) {
            spec = spec.and(
                    ProductSpecification.hasName(request.getName())
            );
        }

        if (request.getPrice() != 0) {
            spec = spec.and(
                    ProductSpecification.hasPrice(request.getPrice())
            );
        }

        return productService.getSpecificProducts(spec);
    }
}
