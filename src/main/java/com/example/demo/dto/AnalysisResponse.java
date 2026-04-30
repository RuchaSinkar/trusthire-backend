package com.example.demo.dto;

import com.example.demo.entity.Verdict;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AnalysisResponse {
    private Long id;
    private String companyName;
    private String domain;
    private int trustScore;
    private Verdict verdict;
    private String verdictLabel;
    private String verdictDescription;
    private List<FlagDetail> flags;
    private Map<String, Integer> scoreBreakdown;
    private LocalDateTime analyzedAt;

    @Data
    @Builder
    public static class FlagDetail {
        private String code;           // e.g. "DOMAIN_TOO_NEW"
        private String label;          // e.g. "Domain is less than 6 months old"
        private int scoreImpact;       // e.g. -25
        private Severity severity;
    }

    public enum Severity { HIGH, MEDIUM, LOW, POSITIVE }
}

