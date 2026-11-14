package com.example.product_search_engine.repository;

import com.example.product_search_engine.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query(value = """
            WITH RECURSIVE category_tree AS (
                SELECT id
                FROM category
                WHERE id = :categoryId

                UNION

                SELECT c.id
                FROM category c
                INNER JOIN category_tree ct
                    ON c.parent_id = ct.id
            )
            SELECT id
            FROM category_tree
            """, nativeQuery = true)
    List<Long> findCategoryAndDescendantIds(
            @Param("categoryId") Long categoryId
    );
}