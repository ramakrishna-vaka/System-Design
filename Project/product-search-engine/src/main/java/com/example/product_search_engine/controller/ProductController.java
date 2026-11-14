package com.example.product_search_engine.controller;

import com.example.product_search_engine.model.Product;
import com.example.product_search_engine.model.ProductSearchRequest;
import com.example.product_search_engine.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService){
        this.productService = productService;
    }

    @PostMapping("/search")
    public List<Product> searchProducts(@ModelAttribute ProductSearchRequest request){
        return productService.searchProducts(request);
    }
}
