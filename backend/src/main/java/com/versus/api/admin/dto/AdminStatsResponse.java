package com.versus.api.admin.dto;

public record AdminStatsResponse(
        long totalUsers,
        long activeUsers,
        long gamesToday,
        long totalQuestions,
        long pendingReports
) {}
