package com.example.demo.controller;

import com.example.demo.dto.CompanyCheckRequest;
import com.example.demo.dto.CompanyCheckResponse;
import com.example.demo.dto.ScamReportDto;
import com.example.demo.service.ScamDetectionService;
import com.example.demo.service.ScamReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scam")
@RequiredArgsConstructor
public class ScamController {

    private final ScamDetectionService scamDetectionService;
    private final ScamReportService    scamReportService;

    // ─────────────────────────────────────────────────────────────────
    // MAIN ENDPOINT — React calls this when user clicks "Check"
    // ─────────────────────────────────────────────────────────────────

    /**
     * POST /api/scam/check    ← PUBLIC, no login required
     *
     * Request body:
     * {
     *   "companyName": "Zorvyn",
     *   "website":     "https://zorvyn.com",
     *   "email":       "hr@zorvyn.com",
     *   "jobText":     "Congratulations! You are selected... accept tonight..."
     * }
     *
     * Response: CompanyCheckResponse with score, status, reasons, breakdown
     */
    @PostMapping("/check")
    public ResponseEntity<CompanyCheckResponse> checkCompany(
            @Valid @RequestBody CompanyCheckRequest request) {
        return ResponseEntity.ok(scamDetectionService.checkCompany(request));
    }

    // ─────────────────────────────────────────────────────────────────
    // COMMUNITY REPORTS
    // ─────────────────────────────────────────────────────────────────

    /**
     * GET /api/scam/reports?page=0&size=10&sort=recent|popular   ← PUBLIC
     */
    @GetMapping("/reports")
    public ResponseEntity<Page<ScamReportDto.Response>> getReports(
            @RequestParam(defaultValue = "0")      int page,
            @RequestParam(defaultValue = "10")     int size,
            @RequestParam(defaultValue = "recent") String sort,
            @AuthenticationPrincipal UserDetails   userDetails) {

        Sort sortSpec = "popular".equals(sort)
                ? Sort.by("upvotes").descending()
                : Sort.by("createdAt").descending();

        String username = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(
                scamReportService.getReports(PageRequest.of(page, size, sortSpec), username));
    }

    /**
     * GET /api/scam/reports/{id}   ← PUBLIC
     */
    @GetMapping("/reports/{id}")
    public ResponseEntity<ScamReportDto.Response> getReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(scamReportService.getById(id, username));
    }

    /**
     * POST /api/scam/reports   ← REQUIRES LOGIN
     * Submit your own scam experience (like yours with Zorvyn)
     */
    @PostMapping("/reports")
    public ResponseEntity<ScamReportDto.Response> createReport(
            @Valid @RequestBody ScamReportDto.CreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scamReportService.create(request, userDetails.getUsername()));
    }

    /**
     * POST /api/scam/reports/{id}/upvote   ← REQUIRES LOGIN
     * Toggle upvote — call again to remove your upvote
     */
    @PostMapping("/reports/{id}/upvote")
    public ResponseEntity<Void> upvoteReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        scamReportService.toggleUpvote(id, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * DELETE /api/scam/reports/{id}   ← REQUIRES LOGIN (own report or admin)
     */
    @DeleteMapping("/reports/{id}")
    public ResponseEntity<Void> deleteReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        scamReportService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}