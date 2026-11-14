package com.example.product_search_engine.model;

import jakarta.persistence.*;

@Entity
public class Category {

    @GeneratedValue
    @Id
    private Long id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    public Category getParent() {
        return parent;
    }

    public void setParent_id(Category parent) {
        this.parent = parent;
    }


    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }


}
