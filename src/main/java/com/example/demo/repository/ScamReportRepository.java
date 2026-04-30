package com.example.demo.repository;

import com.example.demo.entity.ScamReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScamReportRepository extends JpaRepository<ScamReport, Long> {

    // Used by ScamReportService.getReports()
    Page<ScamReport> findAll(Pageable pageable);

    // Used by ScamDetectionService via calculateRisk() — finds reports by domain
    List<ScamReport> findByDomainIgnoreCase(String domain);

    // Used by ScamDetectionService via calculateRisk() — finds reports by company name
    int countByCompanyNameIgnoreCase(String companyName);

    // Check if a user has already upvoted a report (prevents double voting)
    @Query(value = "SELECT COUNT(*) > 0 FROM report_upvotes " +
            "WHERE report_id = :reportId AND user_id = :userId",
            nativeQuery = true)
    boolean hasUserUpvoted(@Param("reportId") Long reportId, @Param("userId") Long userId);

    // Add an upvote — ON CONFLICT DO NOTHING prevents duplicate rows
    @Modifying
    @Query(value = "INSERT INTO report_upvotes (user_id, report_id) VALUES (:userId, :reportId) " +
            "ON CONFLICT DO NOTHING",
            nativeQuery = true)
    void addUpvote(@Param("userId") Long userId, @Param("reportId") Long reportId);

    // Remove an upvote
    @Modifying
    @Query(value = "DELETE FROM report_upvotes WHERE user_id = :userId AND report_id = :reportId",
            nativeQuery = true)
    void removeUpvote(@Param("userId") Long userId, @Param("reportId") Long reportId);
}