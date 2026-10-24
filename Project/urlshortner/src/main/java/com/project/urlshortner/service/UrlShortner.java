package com.project.urlshortner.service;

import com.project.urlshortner.model.UrlData;
import com.project.urlshortner.repository.UrlDataRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class UrlShortner {

    private final UrlDataRepository urlDataRepository;

    public UrlShortner(UrlDataRepository urlDataRepository){
        this.urlDataRepository = urlDataRepository;
    }
    public boolean contains(String shortCode){
        UrlData urlData = urlDataRepository.findById(shortCode)
                .orElse(null);

        if (urlData == null) {
            return false;
        }
        return true;
    }

    public void put(String shortCode, String url){
        UrlData urlData = new UrlData(url, shortCode);
        urlDataRepository.save(urlData);
    }

    public String getUrl(String shortCode) {
        UrlData urlData = urlDataRepository.findById(shortCode)
                .orElse(null);

        if (urlData == null) {
            return null;
        }

        urlDataRepository.incrementViewsByShortCode(shortCode);

        return urlData.getUrl();
    }

    public int getAnalytics(String shortCode) {
        return urlDataRepository.findById(shortCode)
                .map(UrlData::getViews)
                .orElse(0);
    }
}
