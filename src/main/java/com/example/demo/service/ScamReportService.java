package com.example.demo.service;

import com.example.demo.dto.ScamReportDto;
import com.example.demo.entity.ScamReport;
import com.example.demo.entity.User;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.ScamReportRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScamReportService {

    private final ScamReportRepository reportRepository;
    private final UserRepository       userRepository;

    // ─────────────────────────────────────────────────────────────────
    // Called by ScamDetectionService
    // ─────────────────────────────────────────────────────────────────

    /**
     * Returns a negative score penalty based on how many community reports
     * exist for this company name or domain.
     *
     *  0 reports  →   0  (no signal)
     *  1-2 reports → -15 (suspicious)
     *  3+ reports  → -30 (strong scam signal)
     */
    public int calculateRisk(String companyName, String domain) {
        int reportCount = 0;

        // Count by domain first (more precise)
        if (domain != null && !domain.isBlank()) {
            String cleanDomain = normalizeDomain(domain);
            reportCount = reportRepository.findByDomainIgnoreCase(cleanDomain).size();
        }

        // Also count by company name if domain search found nothing
        if (reportCount == 0 && companyName != null && !companyName.isBlank()) {
            reportCount = reportRepository.countByCompanyNameIgnoreCase(companyName);
        }

        if (reportCount == 0) return 0;
        if (reportCount <= 2) {
            log.info("Found {} report(s) for company/domain — mild penalty", reportCount);
            return -15;
        }
        log.info("Found {} reports for company/domain — strong scam signal", reportCount);
        return -30;
    }

    // ─────────────────────────────────────────────────────────────────
    // CRUD — used by ScamReportController
    // ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ScamReportDto.Response> getReports(Pageable pageable, String username) {
        Long userId = resolveUserId(username);
        return reportRepository.findAll(pageable).map(r -> toResponse(r, userId));
    }

    @Transactional(readOnly = true)
    public ScamReportDto.Response getById(Long id, String username) {
        Long userId = resolveUserId(username);
        return toResponse(findById(id), userId);
    }

    @Transactional
    public ScamReportDto.Response create(ScamReportDto.CreateRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ScamReport report = ScamReport.builder()
                .user(user)
                .companyName(request.getCompanyName())
                .domain(normalizeDomain(request.getDomain()))
                .description(request.getDescription())
                .stipendOffered(request.getStipendOffered())
                .experience(request.getExperience())
                .upvotes(0)
                .verified(false)
                .build();

        return toResponse(reportRepository.save(report), user.getId());
    }

    @Transactional
    public void toggleUpvote(Long reportId, String username) {
        User       user   = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ScamReport report = findById(reportId);

        boolean alreadyUpvoted = reportRepository.hasUserUpvoted(reportId, user.getId());
        if (alreadyUpvoted) {
            reportRepository.removeUpvote(user.getId(), reportId);
            report.setUpvotes(Math.max(0, report.getUpvotes() - 1));
        } else {
            reportRepository.addUpvote(user.getId(), reportId);
            report.setUpvotes(report.getUpvotes() + 1);
        }
        reportRepository.save(report);
    }

    @Transactional
    public void delete(Long reportId, String username) {
        User       user   = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ScamReport report = findById(reportId);

        boolean isOwner = report.getUser() != null
                && report.getUser().getId().equals(user.getId());
        boolean isAdmin = "ROLE_ADMIN".equals(user.getRole());

        if (!isOwner && !isAdmin) {
            throw new BadRequestException("You don't have permission to delete this report");
        }
        reportRepository.delete(report);
    }

    // ─────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────

    private ScamReport findById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));
    }

    private Long resolveUserId(String username) {
        if (username == null) return null;
        return userRepository.findByUsername(username).map(User::getId).orElse(null);
    }

    private String normalizeDomain(String domain) {
        if (domain == null || domain.isBlank()) return null;
        return domain.toLowerCase()
                .replace("https://", "").replace("http://", "")
                .replace("www.", "").split("/")[0];
    }

    private ScamReportDto.Response toResponse(ScamReport report, Long currentUserId) {
        boolean upvoted = currentUserId != null
                && reportRepository.hasUserUpvoted(report.getId(), currentUserId);
        String reportedBy = report.getUser() != null
                ? report.getUser().getUsername() : "Anonymous";

        return ScamReportDto.Response.builder()
                .id(report.getId())
                .companyName(report.getCompanyName())
                .domain(report.getDomain())
                .description(report.getDescription())
                .stipendOffered(report.getStipendOffered())
                .experience(report.getExperience())
                .upvotes(report.getUpvotes())
                .verified(report.isVerified())
                .upvotedByCurrentUser(upvoted)
                .reportedBy(reportedBy)
                .createdAt(report.getCreatedAt())
                .build();
    }
}