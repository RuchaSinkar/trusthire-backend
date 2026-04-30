package com.example.demo.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AnalysisRequest {

    // User can provide a URL, raw text, or both
    @Size(max = 2000, message = "URL must be under 2000 characters")
    private String jobUrl;

    @Size(max = 10000, message = "Job text must be under 10,000 characters")
    private String jobText;

    @Size(max = 255, message = "Company name must be under 255 characters")
    private String companyName;
}
