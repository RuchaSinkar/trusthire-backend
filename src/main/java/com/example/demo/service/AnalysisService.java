package com.example.demo.service;

import com.example.demo.dto.AnalysisRequest;
import com.example.demo.dto.AnalysisResponse;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.service.ScoringService.ScoringResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisService {

    private final ScoringService      scoringService;
    private final DomainCheckService  domainCheckService;
    private final CompanyRepository   companyRepository;
    private final AnalysisRepository  analysisRepository;
    private final ScamReportRepository scamReportRepository;
    private final UserRepository      userRepository;

    /**
     * Main entry point. Accepts a job URL and/or pasted job text,
     * runs all detection checks, persists the result, and returns a response.
     *
     * @param request   input (jobUrl + jobText + companyName)
     * @param username  null if anonymous request
     */
    @Transactional
    public AnalysisResponse analyze(AnalysisRequest request, String username) {

        // 1. Extract domain from URL
        String domain = extractDomain(request.getJobUrl());

        // 2. Score the text content
        String       combined = buildText(request);
        ScoringResult result   = scoringService.scoreFromText(combined);

        // 3. Domain-level checks (WHOIS + SafeBrowse)
        Integer domainAgeDays = null;
        boolean isSafe        = true;
        if (domain != null) {
            domainAgeDays = domainCheckService.getDomainAgeDays(domain);
            isSafe        = domainCheckService.isSafe(request.getJobUrl());
        }

        // 4. Community report count for this domain
        int reportCount = (domain != null)
                ? scamReportRepository.findByDomainIgnoreCase(domain).size()
                : 0;

        // 5. Merge all signals into final score
        result = scoringService.applyDomainSignals(result, domainAgeDays, isSafe, reportCount);

        // 6. Persist / update Company record
        Company company = upsertCompany(domain, request.getCompanyName(), result, domainAgeDays, isSafe);

        // 7. Persist Analysis record
        com.example.demo.entity.User user = (username != null)
                ? userRepository.findByUsername(username).orElse(null)
                : null;

        Analysis analysis = Analysis.builder()
                .user(user)
                .company(company)
                .jobUrl(request.getJobUrl())
                .jobText(request.getJobText())
                .companyName(resolvedName(request.getCompanyName(), domain))
                .trustScore(result.score())
                .verdict(result.verdict())
                .flags(result.flags().stream().map(AnalysisResponse.FlagDetail::getCode).toList())
                .scoreBreakdown(result.scoreBreakdown())
                .build();

        Analysis saved = analysisRepository.save(analysis);
        return buildResponse(saved, result, domain);
    }

    @Transactional(readOnly = true)
    public Page<AnalysisResponse> getHistory(String username, Pageable pageable) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return analysisRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(a -> buildResponse(a, null, extractDomain(a.getJobUrl())));
    }

    // ── Helpers ────────────────────────────────────────────────────────

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

    private String buildText(AnalysisRequest req) {
        var sb = new StringBuilder();
        if (req.getCompanyName() != null) sb.append(req.getCompanyName()).append(" ");
        if (req.getJobText()     != null) sb.append(req.getJobText());
        return sb.toString();
    }

    private String resolvedName(String provided, String domain) {
        if (provided != null && !provided.isBlank()) return provided;
        if (domain   != null)                        return domain;
        return "Unknown Company";
    }

    private Company upsertCompany(String domain, String name, ScoringResult r,
                                  Integer ageDays, boolean isSafe) {
        if (domain == null) return null;
        return companyRepository.findByDomain(domain)
                .map(c -> {
                    c.setTrustScore(r.score());
                    c.setVerdict(r.verdict());
                    c.setDomainAgeDays(ageDays);
                    c.setIsSafeBrowse(isSafe);
                    c.setAnalysisCount(c.getAnalysisCount() + 1);
                    c.setLastAnalyzed(LocalDateTime.now());
                    return companyRepository.save(c);
                })
                .orElseGet(() -> companyRepository.save(
                        Company.builder()
                                .domain(domain)
                                .companyName(resolvedName(name, domain))
                                .trustScore(r.score())
                                .verdict(r.verdict())
                                .domainAgeDays(ageDays)
                                .isSafeBrowse(isSafe)
                                .analysisCount(1)
                                .build()
                ));
    }

    private AnalysisResponse buildResponse(Analysis a, ScoringResult r, String domain) {
        List<AnalysisResponse.FlagDetail> flags = (r != null) ? r.flags() : List.of();
        var breakdown = (r != null) ? r.scoreBreakdown() : a.getScoreBreakdown();

        return AnalysisResponse.builder()
                .id(a.getId())
                .companyName(a.getCompanyName())
                .domain(domain)
                .trustScore(a.getTrustScore())
                .verdict(a.getVerdict())
                .verdictLabel(ScoringService.verdictLabel(a.getVerdict()))
                .verdictDescription(ScoringService.verdictDescription(a.getVerdict()))
                .flags(flags)
                .scoreBreakdown(breakdown)
                .analyzedAt(a.getCreatedAt())
                .build();
    }
}