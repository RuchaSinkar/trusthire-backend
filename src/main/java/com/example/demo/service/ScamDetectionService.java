package com.example.demo.service;

import com.example.demo.dto.CompanyCheckRequest;
import com.example.demo.dto.CompanyCheckResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ScamDetectionService {

    private final LinkedinCheckService   linkedinService;
    private final ScamReportService      scamReportService;
    private final DomainCheckService     domainCheckService;
    private final EmailValidationService emailValidationService;

    private static final Pattern TRAINING_FEE = Pattern.compile(
            "(?<![Nn]o\\s)(?<![Ww]ithout\\s)(?<![Ff]ree\\s)" +
                    "(training\\s*fee|registration\\s*fee|security\\s*deposit|pay.*to.*join|joining\\s*fee)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern UPFRONT_PAYMENT = Pattern.compile(
            "send\\s+money|bank\\s+transfer|western\\s+union|upi.*payment|pay\\s+us\\s+first",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PERSONAL_DOCS = Pattern.compile(
            "aadhaar|pan\\s+card|passport\\s+copy|bank\\s+account\\s+details|send.*document",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern URGENCY = Pattern.compile(
            "accept.*tonight|limited\\s+seats|hurry|act\\s+now|offer\\s+expires|immediate\\s+joining",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern GUARANTEED_JOB = Pattern.compile(
            "guaranteed\\s+job|100%\\s+placement|assured\\s+(job|internship)|definitely\\s+hired",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern VAGUE_DESC = Pattern.compile(
            "work\\s+from\\s+home.*earn|easy\\s+money|no\\s+experience\\s+needed|anyone\\s+can\\s+apply",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern HIGH_STIPEND = Pattern.compile(
            "stipend.*[1-9]\\d{5,}|[1-9]\\d{5,}.*stipend|lakh.*per\\s+month.*intern",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CODING_ROUND = Pattern.compile(
            "coding\\s+round|technical\\s+test|hackerrank|leetcode|aptitude\\s+test|coding\\s+challenge",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern STRUCTURED_PROCESS = Pattern.compile(
            "round\\s*1|round\\s*2|technical\\s+interview|hr\\s+round|multiple\\s+rounds",
            Pattern.CASE_INSENSITIVE);

    public CompanyCheckResponse checkCompany(CompanyCheckRequest request) {

        int score = 100;
        List<String>         reasons   = new ArrayList<>();
        Map<String, Integer> breakdown = new LinkedHashMap<>();

        // 1. DOMAIN AGE
        if (request.getWebsite() != null && !request.getWebsite().isBlank()) {
            boolean isNewDomain = domainCheckService.isDomainNew(request.getWebsite());
            if (isNewDomain) {
                score -= 25; breakdown.put("domainAge", -25);
                reasons.add("Domain is less than 6 months old (-25)");
            } else {
                score += 5; breakdown.put("domainAge", +5);
                reasons.add("Domain has an established age (+5)");
            }
        }

        // 2. LINKEDIN
        if (request.getCompanyName() != null && !request.getCompanyName().isBlank()) {
            boolean hasLinkedin = linkedinService.hasLinkedinPage(request.getCompanyName());
            if (!hasLinkedin) {
                score -= 15; breakdown.put("linkedinPresence", -15);
                reasons.add("No LinkedIn company page found (-15)");
            } else {
                score += 5; breakdown.put("linkedinPresence", +5);
                reasons.add("LinkedIn company page exists (+5)");
            }
        }

        // 3. EMAIL DOMAIN
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            int emailRisk = emailValidationService.validateEmail(request.getEmail(), request.getWebsite());
            score += emailRisk; breakdown.put("emailDomain", emailRisk);
            if (emailRisk < 0)
                reasons.add("Contact email uses a free provider like Gmail/Yahoo (" + emailRisk + ")");
            else if (emailRisk > 0)
                reasons.add("Contact email matches the company domain (+" + emailRisk + ")");
        }

        // 4. COMMUNITY REPORTS
        int reportRisk = scamReportService.calculateRisk(request.getCompanyName(), request.getWebsite());
        score += reportRisk; breakdown.put("communityReports", reportRisk);
        if (reportRisk < 0)
            reasons.add("Community has reported this company as suspicious (" + reportRisk + ")");

        // 5. JOB TEXT ANALYSIS
        if (request.getJobText() != null && !request.getJobText().isBlank()) {
            score = analyzeJobText(request.getJobText(), score, reasons, breakdown);
        }

        // 6. CLAMP 0-100
        score = Math.max(0, Math.min(100, score));

        // 7. BUILD RESPONSE
        CompanyCheckResponse response = new CompanyCheckResponse();
        response.setRiskScore(score);
        response.setStatus(toStatus(score));
        response.setStatusLabel(toStatusLabel(score));
        response.setStatusDescription(toStatusDescription(score));
        response.setReasons(reasons);
        response.setScoreBreakdown(breakdown);
        response.setCheckedAt(LocalDateTime.now());
        return response;
    }

    private int analyzeJobText(String text, int score, List<String> reasons, Map<String, Integer> breakdown) {
        if (TRAINING_FEE.matcher(text).find())      { score -= 35; breakdown.put("trainingFee", -35);        reasons.add("Mentions training fees or registration charges (-35)"); }
        if (UPFRONT_PAYMENT.matcher(text).find())   { score -= 35; breakdown.put("upfrontPayment", -35);     reasons.add("Asks for upfront payment or money transfer (-35)"); }
        if (PERSONAL_DOCS.matcher(text).find())     { score -= 30; breakdown.put("personalDocs", -30);       reasons.add("Requests personal documents like Aadhaar or PAN (-30)"); }
        if (URGENCY.matcher(text).find())           { score -= 20; breakdown.put("urgencyLanguage", -20);    reasons.add("Uses urgency tactics like 'accept tonight' (-20)"); }
        if (GUARANTEED_JOB.matcher(text).find())    { score -= 20; breakdown.put("guaranteedJob", -20);      reasons.add("Guarantees selection without a proper process (-20)"); }
        if (VAGUE_DESC.matcher(text).find())        { score -= 15; breakdown.put("vagueDescription", -15);   reasons.add("Job description is vague with no real responsibilities (-15)"); }
        if (HIGH_STIPEND.matcher(text).find())      { score -= 10; breakdown.put("highStipend", -10);        reasons.add("Stipend seems unusually high — possible lure tactic (-10)"); }
        if (CODING_ROUND.matcher(text).find())      { score += 10; breakdown.put("codingRound", +10);        reasons.add("Mentions a coding round or technical test (+10)"); }
        if (STRUCTURED_PROCESS.matcher(text).find()){ score += 10; breakdown.put("structuredProcess", +10);  reasons.add("Describes a multi-stage structured hiring process (+10)"); }
        return score;
    }

    public static String toStatus(int score) {
        if (score >= 70) return "Legit";
        if (score >= 40) return "Suspicious";
        return "Scam";
    }
    public static String toStatusLabel(int score) {
        if (score >= 70) return "Likely Genuine";
        if (score >= 40) return "Suspicious";
        return "Likely Scam";
    }
    public static String toStatusDescription(int score) {
        if (score >= 70) return "This posting shows signs of a legitimate opportunity. Always verify independently before sharing personal details.";
        if (score >= 40) return "Several warning signs detected. Proceed with extreme caution and do additional research before engaging.";
        return "Strong indicators of a fraudulent posting. We strongly recommend not engaging with this company.";
    }
}