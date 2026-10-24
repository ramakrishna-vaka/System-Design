package com.project.urlshortner.model;

public class UrlDTO {

    String website;
    String shortCode;

    public void setWebsite(String website){
        this.website = website;
    }

    public String getWebsite(){
        return website;
    }

    public void setShortCode(String shortCode){
        this.shortCode = shortCode;
    }

    public String getShortCode(){
        return shortCode;
    }
}
