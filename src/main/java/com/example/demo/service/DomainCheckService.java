package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DomainCheckService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.whois.api-key}")
    private String whoisApiKey;

    @Value("${app.whois.base-url}")
    private String whoisBaseUrl;

    @Value("${app.google.safe-browse.api-key}")
    private String safeBrowseApiKey;

    @Value("${app.google.safe-browse.base-url}")
    private String safeBrowseBaseUrl;

    /**
     * Called by ScamDetectionService.
     * Returns true if the domain is less than 6 months (180 days) old.
     */
    @Cacheable(value = "domainAge", key = "#websiteUrl")
    public boolean isDomainNew(String websiteUrl) {
        Integer ageDays = getDomainAgeDays(websiteUrl);
        if (ageDays == null) return false;
        return ageDays < 180;
    }

    /**
     * Returns domain age in days, or null if WHOIS unavailable.
     */
    @Cacheable(value = "domainAgeDays", key = "#websiteUrl")
    public Integer getDomainAgeDays(String websiteUrl) {
        try {
            String domain = extractDomain(websiteUrl);
            if (domain == null) return null;

            String url = whoisBaseUrl
                    + "?apiKey=" + whoisApiKey
                    + "&domainName=" + domain
                    + "&outputFormat=JSON";

            ResponseEntity<String> res = restTemplate.getForEntity(url, String.class);
            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) return null;

            JsonNode root    = objectMapper.readTree(res.getBody());
            String   dateStr = root.path("WhoisRecord")
                    .path("registryData")
                    .path("createdDate")
                    .asText(null);
            if (dateStr == null) return null;

            LocalDate created = LocalDate.parse(
                    dateStr.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
            return (int) ChronoUnit.DAYS.between(created, LocalDate.now());

        } catch (Exception e) {
            log.warn("WHOIS check failed for {}: {}", websiteUrl, e.getMessage());
            return null;
        }
    }

    /**
     * Returns true if URL is safe per Google Safe Browsing. Fails open.
     */
    @Cacheable(value = "safeBrowse", key = "#url")
    public boolean isSafe(String url) {
        if (url == null || url.isBlank()) return true;
        try {
            String apiUrl = safeBrowseBaseUrl + "?key=" + safeBrowseApiKey;
            Map<String, Object> body = Map.of(
                    "client", Map.of("clientId", "trusthire", "clientVersion", "1.0"),
                    "threatInfo", Map.of(
                            "threatTypes",      List.of("MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE"),
                            "platformTypes",    List.of("ANY_PLATFORM"),
                            "threatEntryTypes", List.of("URL"),
                            "threatEntries",    List.of(Map.of("url", url))
                    )
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> res = restTemplate.postForEntity(
                    apiUrl, new HttpEntity<>(body, headers), String.class);
            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) return true;
            return !objectMapper.readTree(res.getBody()).has("matches");
        } catch (Exception e) {
            log.warn("Safe Browsing check failed for {}: {}", url, e.getMessage());
            return true;
        }
    }

    private String extractDomain(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            String clean = url.startsWith("http") ? url : "https://" + url;
            return new URI(clean).getHost();
        } catch (Exception e) {
            log.warn("Cannot parse domain from: {}", url);
            return null;
        }
    }
}