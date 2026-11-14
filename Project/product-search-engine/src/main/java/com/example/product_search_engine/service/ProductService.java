package com.example.product_search_engine.service;

import com.example.product_search_engine.model.Product;
import com.example.product_search_engine.model.ProductSearchRequest;
import com.example.product_search_engine.repository.ProductRepository;
import com.example.product_search_engine.service.filterStrategy.ProductFilterStrategy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final List<ProductFilterStrategy> strategies;

    public ProductService(ProductRepository productRepository,
                          List<ProductFilterStrategy> strategies){
        this.productRepository = productRepository;
        this.strategies = strategies;
    }

    public List<Product> searchProducts(ProductSearchRequest request) {
        Specification<Product> specification =
                (root, query, cb) -> cb.conjunction();

        for (ProductFilterStrategy strategy : strategies) {

            if (strategy.isApplicable(request)) {
                specification = specification.and(
                        strategy.build(request)
                );
            }
        }


        Sort sort = Sort.by(
                Sort.Direction.fromString(
                        request.getSortDirection() == null
                                ? "ASC"
                                : request.getSortDirection()
                ),
                request.getSortBy() == null
                        ? "price"
                        : request.getSortBy()
        );

        return productRepository.findAll(
                specification,
                sort
        );
    }
}
