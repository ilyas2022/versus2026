package com.versus.api.match;

import com.versus.api.common.exception.ApiException;
import com.versus.api.match.domain.Match;
import com.versus.api.match.domain.MatchAnswer;
import com.versus.api.match.domain.MatchPlayer;
import com.versus.api.match.domain.MatchRound;
import com.versus.api.match.dto.*;
import com.versus.api.match.repo.*;
import com.versus.api.questions.domain.Question;
import com.versus.api.questions.repo.QuestionRepository;
import com.versus.api.users.domain.User;
import com.versus.api.users.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private static final int MAX_PAGE_SIZE = 50;

    private final MatchRepository matches;
    private final MatchPlayerRepository matchPlayers;
    private final MatchRoundRepository matchRounds;
    private final MatchAnswerRepository matchAnswers;
    private final QuestionRepository questions;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public Page<MatchHistoryItemResponse> getHistory(UUID userId, int page, int size, GameMode mode) {
        int clampedSize = Math.min(size, MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(page, clampedSize);
        Page<Match> matchPage = mode != null
                ? matches.findFinishedByUserIdAndMode(userId, mode.name(), pageable)
                : matches.findFinishedByUserId(userId, pageable);
        return matchPage.map(match -> toHistoryItem(match, userId));
    }

    @Transactional(readOnly = true)
    public MatchDetailResponse getDetail(UUID matchId, UUID userId) {
        Match match = matches.findById(matchId)
                .orElseThrow(() -> ApiException.notFound("Match not found"));

        matchPlayers.findByIdMatchIdAndIdUserId(matchId, userId)
                .orElseThrow(() -> ApiException.forbidden("You are not a player in this match"));

        List<MatchPlayer> allPlayers = matchPlayers.findByIdMatchId(matchId);
        List<UUID> playerUserIds = allPlayers.stream().map(mp -> mp.getId().getUserId()).toList();
        Map<UUID, User> userMap = users.findAllById(playerUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<MatchPlayerSummary> playerSummaries = allPlayers.stream()
                .map(mp -> {
                    User u = userMap.get(mp.getId().getUserId());
                    String username = u != null ? u.getUsername() : "Unknown";
                    return new MatchPlayerSummary(mp.getId().getUserId(), username, mp.getScore(),
                            mp.getLivesRemaining(), mp.getBestStreakInMatch(), mp.getResult());
                })
                .toList();

        List<MatchRound> rounds = matchRounds.findByMatchIdOrderByRoundNumber(matchId);
        List<UUID> roundIds = rounds.stream().map(MatchRound::getId).toList();

        Map<UUID, Question> questionMap = questions.findAllById(rounds.stream().map(MatchRound::getQuestionId).toList())
                .stream().collect(Collectors.toMap(Question::getId, q -> q));

        List<MatchAnswer> userAnswers = matchAnswers.findByRoundIdIn(roundIds).stream()
                .filter(a -> userId.equals(a.getUserId()))
                .toList();
        Map<UUID, MatchAnswer> answerByRound = userAnswers.stream()
                .collect(Collectors.toMap(MatchAnswer::getRoundId, a -> a));

        List<RoundDetailResponse> roundDetails = rounds.stream()
                .map(round -> {
                    MatchAnswer answer = answerByRound.get(round.getId());
                    Question q = questionMap.get(round.getQuestionId());
                    String questionText = q != null ? q.getText() : "";
                    boolean correct = answer != null && Boolean.TRUE.equals(answer.getIsCorrect());
                    String answerGiven = answer != null && answer.getAnswerGiven() != null ? answer.getAnswerGiven() : "";
                    Double deviation = answer != null ? answer.getDeviation() : null;
                    return new RoundDetailResponse(round.getRoundNumber(), round.getQuestionId(),
                            questionText, correct, answerGiven, deviation);
                })
                .toList();

        return new MatchDetailResponse(match.getId(), match.getMode(), match.getCreatedAt(),
                match.getFinishedAt(), playerSummaries, roundDetails);
    }

    private MatchHistoryItemResponse toHistoryItem(Match match, UUID userId) {
        MatchPlayer player = matchPlayers.findByIdMatchIdAndIdUserId(match.getId(), userId)
                .orElseThrow(() -> ApiException.notFound("Player record not found in match"));

        OpponentSummary opponent = null;
        if (isMultiplayer(match.getMode())) {
            List<MatchPlayer> allPlayers = matchPlayers.findByIdMatchId(match.getId());
            opponent = allPlayers.stream()
                    .filter(mp -> !mp.getId().getUserId().equals(userId))
                    .findFirst()
                    .flatMap(mp -> users.findById(mp.getId().getUserId()))
                    .map(u -> new OpponentSummary(u.getId(), u.getUsername(), u.getAvatarUrl()))
                    .orElse(null);
        }

        return new MatchHistoryItemResponse(
                match.getId(),
                match.getMode(),
                player.getResult(),
                player.getScore(),
                player.getBestStreakInMatch(),
                player.getLivesRemaining(),
                player.getRoundsPlayed(),
                match.getFinishedAt(),
                opponent);
    }

    private boolean isMultiplayer(GameMode mode) {
        return mode == GameMode.BINARY_DUEL || mode == GameMode.PRECISION_DUEL || mode == GameMode.SABOTAGE;
    }
}
