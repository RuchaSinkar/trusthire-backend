package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkedinCheckService {

    private final RestTemplate restTemplate;

    /**
     * Called by ScamDetectionService.
     *
     * Checks if a LinkedIn company page likely exists for this company name
     * by attempting to reach the standard LinkedIn company URL.
     *
     * Returns true  → company page found (legitimate signal)
     * Returns false → no page found (suspicious signal)
     *
     * NOTE: LinkedIn blocks direct API access without OAuth.
     * This implementation uses a lightweight HTTP HEAD request to the
     * public company page URL. A 200 response means the page exists.
     * In production you can upgrade this to use LinkedIn's official API
     * with OAuth credentials for more reliable results.
     */
    @Cacheable(value = "linkedinCheck", key = "#companyName")
    public boolean hasLinkedinPage(String companyName) {
        if (companyName == null || companyName.isBlank()) return false;

        try {
            // Convert company name to LinkedIn slug format
            // "Zorvyn Technologies" → "zorvyn-technologies"
            String slug = companyName.trim()
                    .toLowerCase()
                    .replaceAll("[^a-z0-9\\s]", "")   // remove special chars
                    .replaceAll("\\s+", "-");           // spaces → hyphens

            String linkedinUrl = "https://www.linkedin.com/company/" + slug;

            // HEAD request — just checks if the page exists, doesn't download it
            restTemplate.headForHeaders(linkedinUrl);

            log.info("LinkedIn page found for company: {}", companyName);
            return true;

        } catch (Exception e) {
            // 404 or connection error = page not found
            log.info("No LinkedIn page found for company: {} ({})", companyName, e.getMessage());
            return false;
        }
    }
}