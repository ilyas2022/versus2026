package com.versus.api.match;

import com.versus.api.match.dto.MatchDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Matches", description = "Match history and detail")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @Operation(summary = "Get detailed match information including all rounds",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Match detail returned"),
                    @ApiResponse(responseCode = "404", description = "Match not found"),
                    @ApiResponse(responseCode = "403", description = "Not a participant of this match")
            })
    @GetMapping("/{id}")
    public MatchDetailResponse detail(@PathVariable UUID id,
                                      @AuthenticationPrincipal UUID userId) {
        return matchService.getDetail(id, userId);
    }
}
