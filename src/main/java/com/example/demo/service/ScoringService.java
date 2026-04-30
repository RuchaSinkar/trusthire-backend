package com.example.demo.service;

import com.example.demo.dto.AnalysisResponse;
import com.example.demo.dto.AnalysisResponse.FlagDetail;
import com.example.demo.dto.AnalysisResponse.Severity;
import com.example.demo.entity.Verdict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * ScoringService — the core brain of TrustHire.
 *
 * Starts every posting at BASE_SCORE = 100.
 * Each matched rule adds or subtracts from that score.
 * Final score 0–100 maps to a Verdict:
 *   >= 70  →  LIKELY_GENUINE
 *   40-69  →  SUSPICIOUS
 *   < 40   →  LIKELY_SCAM
 *
 * To add a new detection rule, just append one entry to TEXT_RULES.
 * No other code needs to change.
 */
@Service
@Slf4j
public class ScoringService {

    private static final int BASE_SCORE = 100;

    // ── Rule definition ────────────────────────────────────────────────
    private record Rule(
            String   code,
            String   label,
            int      scoreImpact,
            Severity severity,
            Pattern  pattern
    ) {}

    // ── All text-based detection rules ────────────────────────────────
    private static final List<Rule> TEXT_RULES = List.of(

            // HIGH severity — scam hallmarks
            new Rule("TRAINING_FEES",
                    "Mentions training fees or registration charges",
                    -35, Severity.HIGH,
                    Pattern.compile(
                            "training\\s*fee|registration\\s*fee|security\\s*deposit|pay.*to.*join|joining\\s*fee",
                            Pattern.CASE_INSENSITIVE)),

            new Rule("UPFRONT_PAYMENT",
                    "Asks for upfront payment or money transfer",
                    -35, Severity.HIGH,
                    Pattern.compile(
                            "pay\\s+us|send\\s+money|bank\\s+transfer|western\\s+union|wire\\s+transfer|upi.*payment",
                            Pattern.CASE_INSENSITIVE)),

            new Rule("PERSONAL_DOCS_EARLY",
                    "Requests personal documents (Aadhaar/PAN) at the application stage",
                    -30, Severity.HIGH,
                    Pattern.compile(
                            "aadhaar|pan\\s+card|passport\\s+copy|bank\\s+account\\s+details|send.*document",
                            Pattern.CASE_INSENSITIVE)),

            // MEDIUM severity — strong warning signs
            new Rule("URGENCY_LANGUAGE",
                    "Uses urgency tactics — 'accept tonight', 'limited seats', 'offer expires'",
                    -20, Severity.MEDIUM,
                    Pattern.compile(
                            "tonight|accept\\s+within|limited\\s+seats|hurry|act\\s+now|immediate\\s+joining|offer\\s+expires",
                            Pattern.CASE_INSENSITIVE)),

            new Rule("GUARANTEED_SELECTION",
                    "Guarantees selection without a proper process",
                    -20, Severity.MEDIUM,
                    Pattern.compile(
                            "guaranteed\\s+job|100%\\s+placement|assured\\s+(job|internship)|definitely\\s+hired",
                            Pattern.CASE_INSENSITIVE)),

            new Rule("SUSPICIOUS_EMAIL_DOMAIN",
                    "Contact email uses a free provider (Gmail/Yahoo) instead of a company domain",
                    -20, Severity.MEDIUM,
                    Pattern.compile(
                            "from.*@gmail\\.com|from.*@yahoo\\.com|from.*@hotmail\\.com|reply.*@gmail",
                            Pattern.CASE_INSENSITIVE)),

            new Rule("VAGUE_JOB_DESC",
                    "Job description is vague with no tech stack or responsibilities mentioned",
                    -15, Severity.MEDIUM,
                    Pattern.compile(
                            "work\\s+from\\s+home.*earn|easy\\s+money|no\\s+experience\\s+needed|anyone\\s+can\\s+apply",
                            Pattern.CASE_INSENSITIVE)),

            new Rule("NO_INTERVIEW_PROCESS",
                    "Claims no interview is needed or auto-selects candidates",
                    -15, Severity.MEDIUM,
                    Pattern.compile(
                            "no\\s+interview|directly\\s+selected|selected\\s+without\\s+interview|auto.*select",
                            Pattern.CASE_INSENSITIVE)),

            // LOW severity — mild warning signs
            new Rule("UNUSUALLY_HIGH_STIPEND",
                    "Stipend seems unusually high for an intern role — possible lure tactic",
                    -10, Severity.LOW,
                    Pattern.compile(
                            "stipend.*[1-9]\\d{5,}|[1-9]\\d{5,}.*stipend|lakh.*per\\s+month.*intern",
                            Pattern.CASE_INSENSITIVE)),

            // POSITIVE signals — legitimacy indicators
            new Rule("CODING_ROUND",
                    "Mentions a coding round or technical assessment",
                    +10, Severity.POSITIVE,
                    Pattern.compile(
                            "coding\\s+round|technical\\s+test|hackerrank|leetcode|coding\\s+challenge|aptitude\\s+test",
                            Pattern.CASE_INSENSITIVE)),

            new Rule("STRUCTURED_HIRING",
                    "Describes a structured multi-stage hiring process",
                    +10, Severity.POSITIVE,
                    Pattern.compile(
                            "round\\s*1|round\\s*2|technical\\s+interview|hr\\s+round|multiple\\s+rounds",
                            Pattern.CASE_INSENSITIVE)),

            new Rule("OFFICIAL_EMAIL",
                    "Contact email matches a custom company domain (not a free provider)",
                    +15, Severity.POSITIVE,
                    Pattern.compile(
                            "@(?!gmail|yahoo|hotmail|outlook)[a-zA-Z0-9.-]+\\.(com|in|io|co\\.in)",
                            Pattern.CASE_INSENSITIVE))
    );

    // ── Public API ─────────────────────────────────────────────────────

    /**
     * Score a job posting from its text alone.
     * Call applyDomainSignals() afterwards to fold in URL-based checks.
     */
    public ScoringResult scoreFromText(String text) {
        Map<String, Integer> breakdown = new LinkedHashMap<>();
        List<FlagDetail>     flags     = new ArrayList<>();
        int score = BASE_SCORE;

        if (text == null || text.isBlank()) {
            return new ScoringResult(score, Verdict.UNKNOWN, flags, breakdown);
        }

        for (Rule rule : TEXT_RULES) {
            if (rule.pattern().matcher(text).find()) {
                score += rule.scoreImpact();
                breakdown.put(rule.code(), rule.scoreImpact());
                flags.add(FlagDetail.builder()
                        .code(rule.code())
                        .label(rule.label())
                        .scoreImpact(rule.scoreImpact())
                        .severity(rule.severity())
                        .build());
            }
        }

        score = clamp(score);
        return new ScoringResult(score, verdictFor(score), flags, breakdown);
    }

    /**
     * Apply domain/URL-level signals on top of a text-based result.
     *
     * @param base               result from scoreFromText()
     * @param domainAgeDays      null if WHOIS unavailable
     * @param isSafe             result of Google Safe Browsing check
     * @param communityReports   number of community scam reports for this domain
     */
    public ScoringResult applyDomainSignals(ScoringResult base,
                                            Integer domainAgeDays,
                                            boolean isSafe,
                                            int communityReports) {
        int                  score     = base.score();
        Map<String, Integer> breakdown = new LinkedHashMap<>(base.scoreBreakdown());
        List<FlagDetail>     flags     = new ArrayList<>(base.flags());

        // Domain age
        if (domainAgeDays != null) {
            if (domainAgeDays < 180) {
                score -= 25;
                breakdown.put("DOMAIN_TOO_NEW", -25);
                flags.add(flag("DOMAIN_TOO_NEW",
                        "Domain is less than 6 months old (" + domainAgeDays + " days)",
                        -25, Severity.HIGH));
            } else if (domainAgeDays > 365 * 3) {
                score += 10;
                breakdown.put("DOMAIN_ESTABLISHED", +10);
                flags.add(flag("DOMAIN_ESTABLISHED",
                        "Domain is over 3 years old — established presence",
                        +10, Severity.POSITIVE));
            }
        }

        // Google Safe Browsing
        if (!isSafe) {
            score -= 40;
            breakdown.put("UNSAFE_URL", -40);
            flags.add(flag("UNSAFE_URL",
                    "URL flagged by Google Safe Browsing as dangerous",
                    -40, Severity.HIGH));
        }

        // Community reports
        if (communityReports >= 3) {
            score -= 30;
            breakdown.put("COMMUNITY_REPORTED", -30);
            flags.add(flag("COMMUNITY_REPORTED",
                    communityReports + " community members have reported this company as a scam",
                    -30, Severity.HIGH));
        } else if (communityReports > 0) {
            score -= 15;
            breakdown.put("COMMUNITY_FEW_REPORTS", -15);
            flags.add(flag("COMMUNITY_FEW_REPORTS",
                    communityReports + " user(s) reported this company as suspicious",
                    -15, Severity.MEDIUM));
        }

        score = clamp(score);
        return new ScoringResult(score, verdictFor(score), flags, breakdown);
    }

    // ── Verdict helpers (static so controllers can use them) ───────────

    public static Verdict verdictFor(int score) {
        if (score >= 70) return Verdict.LIKELY_GENUINE;
        if (score >= 40) return Verdict.SUSPICIOUS;
        return Verdict.LIKELY_SCAM;
    }

    public static String verdictLabel(Verdict v) {
        return switch (v) {
            case LIKELY_GENUINE -> "✅ Likely Genuine";
            case SUSPICIOUS     -> "⚠️ Suspicious";
            case LIKELY_SCAM    -> "🚨 Likely Scam";
            default             -> "❓ Unknown";
        };
    }

    public static String verdictDescription(Verdict v) {
        return switch (v) {
            case LIKELY_GENUINE -> "This posting shows signs of a legitimate opportunity. Always verify independently before sharing personal details.";
            case SUSPICIOUS     -> "Several warning signs detected. Proceed with extreme caution and do additional research before engaging.";
            case LIKELY_SCAM    -> "Strong indicators of a fraudulent posting. We strongly recommend not engaging with this company.";
            default             -> "Insufficient information to make a determination.";
        };
    }

    // ── Private helpers ────────────────────────────────────────────────

    private FlagDetail flag(String code, String label, int impact, Severity severity) {
        return FlagDetail.builder()
                .code(code).label(label).scoreImpact(impact).severity(severity)
                .build();
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    // ── Result record ──────────────────────────────────────────────────

    public record ScoringResult(
            int                  score,
            Verdict              verdict,
            List<FlagDetail>     flags,
            Map<String, Integer> scoreBreakdown
    ) {}
}