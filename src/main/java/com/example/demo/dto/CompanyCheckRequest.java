package com.example.demo.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompanyCheckRequest {

    // Company name as it appears in the job posting
    @Size(max = 255, message = "Company name must be under 255 characters")
    private String companyName;

    // The job posting URL (e.g. https://zorvyn.com/careers)
    // Used for domain age check + Google Safe Browsing
    @Size(max = 2000, message = "Website URL must be under 2000 characters")
    private String website;

    // Contact email from the offer letter (e.g. hr@zorvyn.com or hr@gmail.com)
    // Used for email domain validation
    @Size(max = 255, message = "Email must be under 255 characters")
    private String email;

    // The full job description or offer letter text pasted by the user
    // Used for text analysis — training fees, urgency language, coding round etc.
    @Size(max = 10000, message = "Job text must be under 10,000 characters")
    private String jobText;
}