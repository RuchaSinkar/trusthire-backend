package com.example.demo.controller;

import com.example.demo.dto.AnalysisRequest;
import com.example.demo.dto.AnalysisResponse;
import com.example.demo.service.AnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    /**
     * POST /api/analysis/analyze   ← PUBLIC, no JWT required
     *
     * Body (all fields optional but at least one must be present):
     * {
     *   "jobUrl":      "https://zorvyn.com/careers",
     *   "jobText":     "We are hiring SDE interns. Stipend: ₹40,000. Accept tonight.",
     *   "companyName": "Zorvyn"
     * }
     */
    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyze(
            @Valid @RequestBody AnalysisRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = (userDetails != null) ? userDetails.getUsername() : null;
        return ResponseEntity.ok(analysisService.analyze(request, username));
    }

    /**
     * GET /api/analysis/history?page=0&size=10   ← REQUIRES JWT
     * Returns the authenticated user's past analysis results, newest first.
     */
    @GetMapping("/history")
    public ResponseEntity<Page<AnalysisResponse>> history(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                analysisService.getHistory(
                        userDetails.getUsername(),
                        PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }
}