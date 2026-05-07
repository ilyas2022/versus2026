package com.versus.api.users;

import com.versus.api.common.dto.ErrorResponse;
import com.versus.api.match.GameMode;
import com.versus.api.match.MatchService;
import com.versus.api.match.dto.MatchHistoryItemResponse;
import com.versus.api.users.dto.UpdateMeRequest;
import com.versus.api.users.dto.UserMeResponse;
import com.versus.api.users.dto.UserPublicResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Users", description = "User profile management")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final MatchService matchService;

    @Operation(summary = "Get the authenticated user's full profile",
            responses = @ApiResponse(responseCode = "200", description = "Profile returned"))
    @GetMapping("/me")
    public UserMeResponse me(@AuthenticationPrincipal UUID userId) {
        return userService.getMe(userId);
    }

    @Operation(summary = "Update the authenticated user's profile",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Profile updated"),
                    @ApiResponse(responseCode = "400", description = "Validation error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Username already taken",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    @PutMapping("/me")
    public UserMeResponse updateMe(@AuthenticationPrincipal UUID userId,
                                   @Valid @RequestBody UpdateMeRequest req) {
        return userService.updateMe(userId, req);
    }

    @Operation(summary = "Get the authenticated user's match history (paginated)",
            responses = @ApiResponse(responseCode = "200", description = "History returned"))
    @GetMapping("/me/history")
    public Page<MatchHistoryItemResponse> history(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) GameMode mode) {
        return matchService.getHistory(userId, page, size, mode);
    }

    @Operation(summary = "Get a user's public profile by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Public profile returned"),
                    @ApiResponse(responseCode = "404", description = "User not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    @GetMapping("/{id}")
    public UserPublicResponse byId(@PathVariable("id") UUID id) {
        return userService.getPublic(id);
    }
}
