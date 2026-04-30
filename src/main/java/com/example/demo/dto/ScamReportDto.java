package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

public class ScamReportDto {

    @Data
    public static class CreateRequest {
        @NotBlank
        @Size(max = 255)
        private String companyName;

        @Size(max = 255)
        private String domain;

        @NotBlank
        @Size(min = 30, max = 5000, message = "Description must be between 30 and 5000 characters")
        private String description;

        @Size(max = 100)
        private String stipendOffered;

        @Size(max = 3000)
        private String experience;
    }

    @Data
    @Builder
    public static class Response {
        private Long id;
        private String companyName;
        private String domain;
        private String description;
        private String stipendOffered;
        private String experience;
        private int upvotes;
        private boolean verified;
        private boolean upvotedByCurrentUser;
        private String reportedBy;
        private LocalDateTime createdAt;
    }
}

