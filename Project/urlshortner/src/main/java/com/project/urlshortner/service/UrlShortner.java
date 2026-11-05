package com.project.urlshortner.service;

import com.project.urlshortner.model.UrlData;
import com.project.urlshortner.repository.UrlDataRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class UrlShortner {

    private final UrlDataRepository urlDataRepository;

    private static final int MAX_RETRIES = 5;

    private static final String CHARACTERS =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final int CODE_LENGTH = 12;

    private static final SecureRandom RANDOM = new SecureRandom();

    public UrlShortner(UrlDataRepository urlDataRepository) {
        this.urlDataRepository = urlDataRepository;
    }

    public String generateAndSaveShortCode(String url) {

        for (int i = 0; i < MAX_RETRIES; i++) {

            StringBuilder code = new StringBuilder(CODE_LENGTH);

            for (int j = 0; j < CODE_LENGTH; j++) {
                int index = RANDOM.nextInt(CHARACTERS.length());
                code.append(CHARACTERS.charAt(index));
            }

            String shortCode = code.toString();

            try {
                UrlData urlData = new UrlData(url, shortCode);
                urlDataRepository.saveAndFlush(urlData);

                return shortCode;

            } catch (DataIntegrityViolationException exception) {
                // A duplicate short code may have been generated.
                // Generate another code and retry.
            }
        }

        throw new IllegalStateException(
                "Unable to generate a unique short code after "
                        + MAX_RETRIES + " attempts"
        );
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