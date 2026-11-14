package com.example.product_search_engine.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "product_attributes",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"product_id", "attribute_name"}
                )
        }
)
public class ProductAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "attribute_name", nullable = false)
    private String attributeName;

    @Column(name = "attribute_value", nullable = false)
    private String attributeValue;
}
