package com.example.demo.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class CompanyCheckResponse {

    private int riskScore;

    // Legit / Suspicious / Scam
    private String status;

    // User-friendly label (Safe, Be Careful, Likely Scam)
    private String statusLabel;

    private String statusDescription;

    // Why this decision was made
    private List<String> reasons;

    // Breakdown of score (example: domain:-25, linkedin:+5)
    private Map<String, Integer> scoreBreakdown;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime checkedAt;
}