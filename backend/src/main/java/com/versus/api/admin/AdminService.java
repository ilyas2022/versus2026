package com.versus.api.admin;

import com.versus.api.admin.dto.AdminStatsResponse;
import com.versus.api.admin.dto.AdminUserResponse;
import com.versus.api.common.exception.ApiException;
import com.versus.api.match.repo.MatchRepository;
import com.versus.api.moderation.ReportStatus;
import com.versus.api.moderation.repo.QuestionReportRepository;
import com.versus.api.questions.QuestionStatus;
import com.versus.api.questions.QuestionType;
import com.versus.api.questions.repo.QuestionRepository;
import com.versus.api.users.Role;
import com.versus.api.users.UserStatus;
import com.versus.api.users.domain.User;
import com.versus.api.users.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository users;
    private final MatchRepository matches;
    private final QuestionRepository questions;
    private final QuestionReportRepository reports;

    // ── Users ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUsers(String search, String roleStr, Pageable pageable) {
        Role role = null;
        if (roleStr != null && !roleStr.isBlank()) {
            try { role = Role.valueOf(roleStr.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        return users.findAdminUsers(role, search, pageable).map(this::toUserResponse);
    }

    @Transactional
    public AdminUserResponse updateRole(UUID userId, Role role) {
        User user = findActiveUser(userId);
        user.setRole(role);
        return toUserResponse(users.save(user));
    }

    @Transactional
    public AdminUserResponse updateStatus(UUID userId, boolean active) {
        User user = findActiveUser(userId);
        user.setIsActive(active);
        return toUserResponse(users.save(user));
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        long totalUsers   = users.count();
        long activeUsers  = users.countByIsActiveTrue();
        long gamesToday   = matches.countByCreatedAtGreaterThanEqual(todayStart());
        long totalBinary  = questions.countByStatusAndType(QuestionStatus.ACTIVE, QuestionType.BINARY);
        long totalNumeric = questions.countByStatusAndType(QuestionStatus.ACTIVE, QuestionType.NUMERIC);
        long pendingReports = reports.countByStatus(ReportStatus.PENDING);
        return new AdminStatsResponse(totalUsers, activeUsers, gamesToday,
                totalBinary + totalNumeric, pendingReports);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User findActiveUser(UUID id) {
        User user = users.findById(id)
                .orElseThrow(() -> ApiException.notFound("User not found: " + id));
        if (user.getStatus() == UserStatus.DELETED) {
            throw ApiException.notFound("User not found: " + id);
        }
        return user;
    }

    private AdminUserResponse toUserResponse(User u) {
        return new AdminUserResponse(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getAvatarUrl(),
                u.getRole().name(),
                Boolean.TRUE.equals(u.getIsActive()),
                u.getCreatedAt()
        );
    }

    private Instant todayStart() {
        return ZonedDateTime.now(ZoneOffset.UTC)
                .toLocalDate()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
    }
}
