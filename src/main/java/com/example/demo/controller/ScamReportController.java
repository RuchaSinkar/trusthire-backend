package com.example.demo.controller;

import com.example.demo.dto.ScamReportDto;
import com.example.demo.service.ScamReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ScamReportController {

    private final ScamReportService reportService;

    /**
     * GET /api/reports?page=0&size=10&sort=recent|popular   ← PUBLIC
     */
    @GetMapping
    public ResponseEntity<Page<ScamReportDto.Response>> getAll(
            @RequestParam(defaultValue = "0")      int page,
            @RequestParam(defaultValue = "10")     int size,
            @RequestParam(defaultValue = "recent") String sort,
            @AuthenticationPrincipal UserDetails   userDetails) {

        Sort sortSpec = "popular".equals(sort)
                ? Sort.by("upvotes").descending()
                : Sort.by("createdAt").descending();

        String username = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(
                reportService.getReports(PageRequest.of(page, size, sortSpec), username));
    }

    /**
     * GET /api/reports/{id}   ← PUBLIC
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScamReportDto.Response> getOne(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(reportService.getById(id, username));
    }

    /**
     * POST /api/reports   ← REQUIRES JWT
     * Submit a community scam report.
     */
    @PostMapping
    public ResponseEntity<ScamReportDto.Response> create(
            @Valid @RequestBody ScamReportDto.CreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.create(request, userDetails.getUsername()));
    }

    /**
     * POST /api/reports/{id}/upvote   ← REQUIRES JWT
     * Toggle upvote on a report (upvote if not voted, un-upvote if already voted).
     */
    @PostMapping("/{id}/upvote")
    public ResponseEntity<Void> upvote(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        reportService.toggleUpvote(id, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * DELETE /api/reports/{id}   ← REQUIRES JWT (own report or ADMIN)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        reportService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}