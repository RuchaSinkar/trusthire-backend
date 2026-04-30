package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String domain;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "trust_score", nullable = false)
    private int trustScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Verdict verdict;

    @Column(name = "domain_age_days")
    private Integer domainAgeDays;

    @Column(name = "is_safe_browse")
    private Boolean isSafeBrowse;

    @Column(name = "analysis_count", nullable = false)
    private int analysisCount;

    @Column(name = "last_analyzed", nullable = false)
    private LocalDateTime lastAnalyzed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastAnalyzed = LocalDateTime.now();
    }
}

