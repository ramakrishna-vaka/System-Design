package com.example.product_search_engine.model;

import java.math.BigDecimal;
import java.util.List;

public class ProductSearchRequest {

    // Searching
    private String keyword;

    //Filtering
    private Long categoryId;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private List<String> brands;

    private List<String> colors;

    // Sorting
    private String sortBy;
    private String sortDirection;

    public Long getCategoryId(){
        return categoryId;
    }

    public BigDecimal getMinPrice(){
        return minPrice;
    }

    public BigDecimal getMaxPrice(){
        return maxPrice;
    }

    public List<String> getBrands(){
        return brands;
    }

    public List<String> getColors(){
        return colors;
    }

    public String getSortBy(){
        return sortBy;
    }

    public String getSortDirection(){
        return sortDirection;
    }

    public String getKeyword(){
        return keyword;
    }

    public void setKeyword(String keyword){
        this.keyword = keyword;
    }
}