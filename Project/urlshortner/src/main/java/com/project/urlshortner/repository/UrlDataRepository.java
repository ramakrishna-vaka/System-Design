package com.project.urlshortner.repository;

import com.project.urlshortner.model.UrlData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UrlDataRepository extends JpaRepository<UrlData, String> {

    @Modifying
    @Transactional
    @Query("""
           UPDATE UrlData u
           SET u.views = u.views + 1
           WHERE u.shortCode = :shortCode
           """)
    int incrementViewsByShortCode(@Param("shortCode") String shortCode);
}
