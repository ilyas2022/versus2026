package com.versus.api.admin;

import com.versus.api.admin.dto.AdminStatsResponse;
import com.versus.api.admin.dto.AdminUserResponse;
import com.versus.api.admin.dto.UpdateRoleRequest;
import com.versus.api.admin.dto.UpdateStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Admin — Users", description = "User management (ADMIN only)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "List users with optional search and role filter")
    @GetMapping("/users")
    public Page<AdminUserResponse> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20) Pageable pageable) {
        return adminService.getUsers(search, role, pageable);
    }

    @Operation(summary = "Update a user's role")
    @PutMapping("/users/{id}/role")
    public AdminUserResponse updateRole(@PathVariable UUID id,
                                        @Valid @RequestBody UpdateRoleRequest req) {
        return adminService.updateRole(id, req.role());
    }

    @Operation(summary = "Update a user's active status (suspend / unsuspend)")
    @PutMapping("/users/{id}/status")
    public AdminUserResponse updateStatus(@PathVariable UUID id,
                                          @Valid @RequestBody UpdateStatusRequest req) {
        return adminService.updateStatus(id, req.active());
    }

    @Operation(summary = "Dashboard KPI stats")
    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        return adminService.getStats();
    }
}
