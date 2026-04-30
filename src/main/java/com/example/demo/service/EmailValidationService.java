package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Set;

@Service
@Slf4j
public class EmailValidationService {

    // Free email providers — using one of these instead of a company domain is a red flag
    private static final Set<String> FREE_PROVIDERS = Set.of(
            "gmail.com", "yahoo.com", "yahoo.in", "hotmail.com",
            "outlook.com", "rediffmail.com", "yopmail.com", "protonmail.com"
    );

    /**
     * Called by ScamDetectionService.
     *
     * Compares the email domain against the company's website domain.
     *
     * Returns:
     *   +15  → email domain matches the company website (e.g. hr@zorvyn.com + zorvyn.com)
     *   -20  → email uses a free provider (e.g. hr@gmail.com)
     *     0  → can't determine (missing data)
     */
    public int validateEmail(String email, String websiteUrl) {
        if (email == null || email.isBlank()) return 0;

        String emailDomain = extractEmailDomain(email);
        if (emailDomain == null) return 0;

        // Check if it's a free provider
        if (FREE_PROVIDERS.contains(emailDomain.toLowerCase())) {
            log.info("Email {} uses free provider — suspicious", email);
            return -20;
        }

        // If we have a website, check if the email domain matches it
        if (websiteUrl != null && !websiteUrl.isBlank()) {
            String websiteDomain = extractWebsiteDomain(websiteUrl);
            if (websiteDomain != null && emailDomain.equalsIgnoreCase(websiteDomain)) {
                log.info("Email {} matches company domain {} — positive signal", email, websiteDomain);
                return +15;
            }
        }

        // Custom domain but doesn't match the website — neutral
        return 0;
    }

    // "hr@zorvyn.com" → "zorvyn.com"
    private String extractEmailDomain(String email) {
        try {
            int atIndex = email.indexOf('@');
            if (atIndex < 0 || atIndex == email.length() - 1) return null;
            return email.substring(atIndex + 1).trim().toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }

    // "https://zorvyn.com/careers" → "zorvyn.com"
    private String extractWebsiteDomain(String url) {
        try {
            String clean = url.startsWith("http") ? url : "https://" + url;
            String host  = new URI(clean).getHost();
            if (host == null) return null;
            // Strip www. prefix
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            log.warn("Cannot parse website domain from: {}", url);
            return null;
        }
    }
}