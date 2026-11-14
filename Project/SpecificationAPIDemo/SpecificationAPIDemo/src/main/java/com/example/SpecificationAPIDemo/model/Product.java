package com.example.SpecificationAPIDemo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Product {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private String category;

    private int price;

    private String brand;

    public void setName(String name){
        this.name = name;
    }

    public void setCategory(String category){
        this.category = category;
    }

    public void setPrice(int price){
        this.price = price;
    }

    public void setBrand(String brand){
        this.brand = brand;
    }

    public String getName(){
        return name;
    }

    public String getCategory(){
        return category;
    }

    public String getBrand(){
        return brand;
    }

    public int getPrice(){
        return price;
    }
}
