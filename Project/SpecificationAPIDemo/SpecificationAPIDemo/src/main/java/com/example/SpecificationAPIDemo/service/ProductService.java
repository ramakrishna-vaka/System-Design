package com.example.SpecificationAPIDemo.service;

import com.example.SpecificationAPIDemo.model.Product;
import com.example.SpecificationAPIDemo.repository.ProductRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository){
        this.productRepository = productRepository;
    }


    public List<Product> getAllProducts(){
        return productRepository.findAll();
    }

    public List<Product> getSpecificProducts(Specification<Product> spec){
        return productRepository.findAll(spec);
    }
}
