
package com.project.urlshortner.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "url_data")
public class UrlData {

    @Id
    @Column(name = "short_code")
    private String shortCode;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private int views = 0;

    protected UrlData() {
        // Required by JPA
    }

    public UrlData(String url, String shortCode) {
        this.url = url;
        this.shortCode = shortCode;
    }

    public String getUrl() {
        return url;
    }

    public String getShortCode() {
        return shortCode;
    }

    public int getViews() {
        return views;
    }

    public void addViews() {
        this.views++;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public void setViews(int views) {
        this.views = views;
    }
}
