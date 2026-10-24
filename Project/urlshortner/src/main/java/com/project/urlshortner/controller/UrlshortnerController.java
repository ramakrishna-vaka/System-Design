package com.project.urlshortner.controller;

import com.project.urlshortner.model.UrlDTO;
import com.project.urlshortner.service.UrlShortner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class UrlshortnerController {

    private final UrlShortner urlShortner;

    public UrlshortnerController(UrlShortner urlShortner){
        this.urlShortner = urlShortner;
    }

    @PostMapping("/shortCode")
    public ResponseEntity<?> analytics(@RequestBody UrlDTO urlDTO) {
        if(urlDTO.getWebsite() == null || urlDTO.getWebsite().isBlank()){
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "url should not be empty"));
        }

        String shortCode = "generateShortCodeMethod";

        while(urlShortner.contains(shortCode)) {
            shortCode = "generateShortCodeMethod";
        }

        urlShortner.put(shortCode, urlDTO.getWebsite());
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", shortCode));
    }

    @PostMapping("/analytics")
    public ResponseEntity<?> shortCode(@RequestBody UrlDTO urlDTO) {
        if(urlDTO.getShortCode() == null || urlDTO.getShortCode().isBlank()){
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Short code should not be empty"));
        }

        int views = urlShortner.getAnalytics(urlDTO.getShortCode());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("views", views));
    }

    @PostMapping("/redirect")
    public ResponseEntity<?> redirect(@RequestBody UrlDTO urlDTO) {
        if(urlDTO.getShortCode() == null || urlDTO.getShortCode().isBlank()){
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Short code should not be empty"));
        }


        String url = urlShortner.getUrl(urlDTO.getShortCode());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("url", url));
    }
}

