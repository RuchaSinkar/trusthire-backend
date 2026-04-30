package com.example.demo.entity;

public enum Verdict {
    LIKELY_GENUINE,   // score >= 70
    SUSPICIOUS,       // score 40-69
    LIKELY_SCAM,      // score < 40
    UNKNOWN
}
