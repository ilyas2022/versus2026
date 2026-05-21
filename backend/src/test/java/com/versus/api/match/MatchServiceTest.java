package com.versus.api.match;

import com.versus.api.common.exception.ApiException;
import com.versus.api.common.exception.ErrorCode;
import com.versus.api.match.domain.Match;
import com.versus.api.match.domain.MatchAnswer;
import com.versus.api.match.domain.MatchPlayer;
import com.versus.api.match.domain.MatchPlayerId;
import com.versus.api.match.domain.MatchRound;
import com.versus.api.match.dto.MatchDetailResponse;
import com.versus.api.match.dto.MatchHistoryItemResponse;
import com.versus.api.match.repo.MatchAnswerRepository;
import com.versus.api.match.repo.MatchPlayerRepository;
import com.versus.api.match.repo.MatchRepository;
import com.versus.api.match.repo.MatchRoundRepository;
import com.versus.api.questions.QuestionStatus;
import com.versus.api.questions.QuestionType;
import com.versus.api.questions.domain.Question;
import com.versus.api.questions.repo.QuestionRepository;
import com.versus.api.users.Role;
import com.versus.api.users.domain.User;
import com.versus.api.users.repo.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("MatchService")
@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock MatchRepository matches;
    @Mock MatchPlayerRepository matchPlayers;
    @Mock MatchRoundRepository matchRounds;
    @Mock MatchAnswerRepository matchAnswers;
    @Mock QuestionRepository questions;
    @Mock UserRepository users;

    @InjectMocks MatchService matchService;

    static final UUID USER_ID  = UUID.fromString("aaaa0000-0000-0000-0000-000000000001");
    static final UUID MATCH_ID = UUID.fromString("bbbb0000-0000-0000-0000-000000000002");

    private Match match(UUID id, GameMode mode) {
        return Match.builder()
                .id(id).mode(mode)
                .createdAt(Instant.now()).finishedAt(Instant.now())
                .build();
    }

    private MatchPlayer player(UUID matchId, UUID userId, int score, MatchResult result) {
        return MatchPlayer.builder()
                .id(new MatchPlayerId(matchId, userId))
                .score(score).livesRemaining(2)
                .currentStreak(1).bestStreakInMatch(3)
                .roundsPlayed(5).result(result)
                .build();
    }

    private User user(UUID id, String username) {
        return User.builder()
                .id(id).username(username).email(username + "@test.com")
                .passwordHash("x").role(Role.PLAYER)
                .isActive(true).createdAt(Instant.now())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // getHistory
    // ═══════════════════════════════════════════════════════════════════════

    @DisplayName("getHistory")
    @Nested
    class GetHistory {

        @DisplayName("Sin filtro de modo usa findFinishedByUserId")
        @Test
        void sinFiltro_usaFindFinishedByUserId() {
            when(matches.findFinishedByUserId(eq(USER_ID), any())).thenReturn(Page.empty());

            matchService.getHistory(USER_ID, 0, 20, null);

            verify(matches).findFinishedByUserId(eq(USER_ID), any());
            verify(matches, never()).findFinishedByUserIdAndMode(any(), any(), any());
        }

        @DisplayName("Con filtro de modo usa findFinishedByUserIdAndMode")
        @Test
        void conFiltroModo_usaFindFinishedByUserIdAndMode() {
            when(matches.findFinishedByUserIdAndMode(eq(USER_ID), eq("PRECISION"), any()))
                    .thenReturn(Page.empty());

            matchService.getHistory(USER_ID, 0, 20, GameMode.PRECISION);

            verify(matches).findFinishedByUserIdAndMode(eq(USER_ID), eq("PRECISION"), any());
            verify(matches, never()).findFinishedByUserId(any(), any());
        }

        @DisplayName("Resultado vacío devuelve página vacía sin llamar a MatchPlayerRepository")
        @Test
        void resultadoVacio_devuelvePageVacia() {
            when(matches.findFinishedByUserId(any(), any())).thenReturn(Page.empty());

            Page<MatchHistoryItemResponse> result = matchService.getHistory(USER_ID, 0, 20, null);

            assertThat(result.getTotalElements()).isZero();
            verify(matchPlayers, never()).findByIdMatchIdAndIdUserId(any(), any());
        }

        @DisplayName("Size superior a 50 se clampea a 50")
        @Test
        void sizeMaximoSuperado_seClampea_a_50() {
            when(matches.findFinishedByUserId(any(), any())).thenReturn(Page.empty());
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            matchService.getHistory(USER_ID, 0, 200, null);

            verify(matches).findFinishedByUserId(any(), captor.capture());
            assertThat(captor.getValue().getPageSize()).isEqualTo(50);
        }

        @DisplayName("Size igual a 50 no se modifica")
        @Test
        void sizeIgual_50_noSeCambia() {
            when(matches.findFinishedByUserId(any(), any())).thenReturn(Page.empty());
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            matchService.getHistory(USER_ID, 0, 50, null);

            verify(matches).findFinishedByUserId(any(), captor.capture());
            assertThat(captor.getValue().getPageSize()).isEqualTo(50);
        }

        @DisplayName("Size inferior a 50 se respeta")
        @Test
        void sizeInferior_50_seRespeta() {
            when(matches.findFinishedByUserId(any(), any())).thenReturn(Page.empty());
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            matchService.getHistory(USER_ID, 0, 10, null);

            verify(matches).findFinishedByUserId(any(), captor.capture());
            assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        }

        @DisplayName("Partida solitaria: opponent es null")
        @Test
        void partida_solitaria_opponentEsNull() {
            Match m = match(MATCH_ID, GameMode.SURVIVAL);
            MatchPlayer mp = player(MATCH_ID, USER_ID, 300, MatchResult.WIN);

            when(matches.findFinishedByUserId(any(), any()))
                    .thenReturn(new PageImpl<>(List.of(m)));
            when(matchPlayers.findByIdMatchIdAndIdUserId(MATCH_ID, USER_ID))
                    .thenReturn(Optional.of(mp));

            Page<MatchHistoryItemResponse> result = matchService.getHistory(USER_ID, 0, 20, null);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).opponent()).isNull();
        }

        @DisplayName("Partida multijugador incluye opponent con username")
        @Test
        void partida_multijugador_incluyeOpponent() {
            UUID opponentId = UUID.randomUUID();
            Match m = match(MATCH_ID, GameMode.BINARY_DUEL);
            MatchPlayer myPlayer    = player(MATCH_ID, USER_ID, 300, MatchResult.WIN);
            MatchPlayer theirPlayer = player(MATCH_ID, opponentId, 200, MatchResult.LOSS);

            when(matches.findFinishedByUserId(any(), any()))
                    .thenReturn(new PageImpl<>(List.of(m)));
            when(matchPlayers.findByIdMatchIdAndIdUserId(MATCH_ID, USER_ID))
                    .thenReturn(Optional.of(myPlayer));
            when(matchPlayers.findByIdMatchId(MATCH_ID))
                    .thenReturn(List.of(myPlayer, theirPlayer));
            when(users.findById(opponentId))
                    .thenReturn(Optional.of(user(opponentId, "Rival")));

            Page<MatchHistoryItemResponse> result = matchService.getHistory(USER_ID, 0, 20, null);

            assertThat(result.getContent().get(0).opponent()).isNotNull();
            assertThat(result.getContent().get(0).opponent().username()).isEqualTo("Rival");
        }

        @DisplayName("Mapea campos básicos correctamente")
        @Test
        void mapeaCamposCorrectos() {
            Match m = match(MATCH_ID, GameMode.SURVIVAL);
            MatchPlayer mp = player(MATCH_ID, USER_ID, 450, MatchResult.WIN);

            when(matches.findFinishedByUserId(any(), any()))
                    .thenReturn(new PageImpl<>(List.of(m)));
            when(matchPlayers.findByIdMatchIdAndIdUserId(MATCH_ID, USER_ID))
                    .thenReturn(Optional.of(mp));

            MatchHistoryItemResponse item = matchService.getHistory(USER_ID, 0, 20, null)
                    .getContent().get(0);

            assertThat(item.id()).isEqualTo(MATCH_ID);
            assertThat(item.mode()).isEqualTo(GameMode.SURVIVAL);
            assertThat(item.result()).isEqualTo(MatchResult.WIN);
            assertThat(item.score()).isEqualTo(450);
            assertThat(item.finishedAt()).isNotNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // getDetail
    // ═══════════════════════════════════════════════════════════════════════

    @DisplayName("getDetail")
    @Nested
    class GetDetail {

        @DisplayName("Partida no encontrada lanza NOT_FOUND")
        @Test
        void partidaNoEncontrada_lanzaNotFound() {
            when(matches.findById(MATCH_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.getDetail(MATCH_ID, USER_ID))
                    .isInstanceOf(ApiException.class)
                    .satisfies(e -> assertThat(((ApiException) e).getCode())
                            .isEqualTo(ErrorCode.NOT_FOUND));
        }

        @DisplayName("Usuario no participante lanza FORBIDDEN")
        @Test
        void usuarioNoParticipante_lanzaForbidden() {
            when(matches.findById(MATCH_ID))
                    .thenReturn(Optional.of(match(MATCH_ID, GameMode.SURVIVAL)));
            when(matchPlayers.findByIdMatchIdAndIdUserId(MATCH_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.getDetail(MATCH_ID, USER_ID))
                    .isInstanceOf(ApiException.class)
                    .satisfies(e -> assertThat(((ApiException) e).getCode())
                            .isEqualTo(ErrorCode.FORBIDDEN));
        }

        @DisplayName("Éxito: devuelve detalle completo con jugadores y rondas")
        @Test
        void exitoso_devuelveDetalleCompleto() {
            UUID opponentId = UUID.randomUUID();
            UUID roundId1   = UUID.randomUUID();
            UUID roundId2   = UUID.randomUUID();
            UUID qId1       = UUID.randomUUID();
            UUID qId2       = UUID.randomUUID();

            MatchPlayer myPlayer    = player(MATCH_ID, USER_ID, 300, MatchResult.WIN);
            MatchPlayer theirPlayer = player(MATCH_ID, opponentId, 200, MatchResult.LOSS);

            MatchRound round1 = MatchRound.builder().id(roundId1).matchId(MATCH_ID)
                    .questionId(qId1).roundNumber(1).createdAt(Instant.now()).build();
            MatchRound round2 = MatchRound.builder().id(roundId2).matchId(MATCH_ID)
                    .questionId(qId2).roundNumber(2).createdAt(Instant.now()).build();

            MatchAnswer answer1 = MatchAnswer.builder().roundId(roundId1).userId(USER_ID)
                    .answerGiven("42").isCorrect(true).deviation(1.5).lifeDelta(1)
                    .answeredAt(Instant.now()).build();
            MatchAnswer answer2 = MatchAnswer.builder().roundId(roundId2).userId(USER_ID)
                    .answerGiven("100").isCorrect(false).deviation(5.0).lifeDelta(-1)
                    .answeredAt(Instant.now()).build();

            Question q1 = Question.builder().id(qId1).text("¿Pregunta 1?")
                    .type(QuestionType.NUMERIC).status(QuestionStatus.ACTIVE).build();
            Question q2 = Question.builder().id(qId2).text("¿Pregunta 2?")
                    .type(QuestionType.NUMERIC).status(QuestionStatus.ACTIVE).build();

            when(matches.findById(MATCH_ID))
                    .thenReturn(Optional.of(match(MATCH_ID, GameMode.PRECISION)));
            when(matchPlayers.findByIdMatchIdAndIdUserId(MATCH_ID, USER_ID))
                    .thenReturn(Optional.of(myPlayer));
            when(matchPlayers.findByIdMatchId(MATCH_ID))
                    .thenReturn(List.of(myPlayer, theirPlayer));
            when(users.findAllById(any()))
                    .thenReturn(List.of(user(USER_ID, "Player1"), user(opponentId, "Player2")));
            when(matchRounds.findByMatchIdOrderByRoundNumber(MATCH_ID))
                    .thenReturn(List.of(round1, round2));
            when(questions.findAllById(any())).thenReturn(List.of(q1, q2));
            when(matchAnswers.findByRoundIdIn(any())).thenReturn(List.of(answer1, answer2));

            MatchDetailResponse response = matchService.getDetail(MATCH_ID, USER_ID);

            assertThat(response.id()).isEqualTo(MATCH_ID);
            assertThat(response.players()).hasSize(2);
            assertThat(response.rounds()).hasSize(2);
            assertThat(response.rounds().get(0).correct()).isTrue();
            assertThat(response.rounds().get(1).correct()).isFalse();
        }

        @DisplayName("Rondas sin respuestas: correct=false y answerGiven vacío")
        @Test
        void rondasSinRespuestas_correctEsFalse_answerGivenVacio() {
            UUID roundId    = UUID.randomUUID();
            UUID questionId = UUID.randomUUID();

            MatchPlayer mp = player(MATCH_ID, USER_ID, 0, MatchResult.LOSS);
            MatchRound round = MatchRound.builder().id(roundId).matchId(MATCH_ID)
                    .questionId(questionId).roundNumber(1).createdAt(Instant.now()).build();
            Question q = Question.builder().id(questionId).text("¿Texto?")
                    .type(QuestionType.BINARY).status(QuestionStatus.ACTIVE).build();

            when(matches.findById(MATCH_ID))
                    .thenReturn(Optional.of(match(MATCH_ID, GameMode.SURVIVAL)));
            when(matchPlayers.findByIdMatchIdAndIdUserId(MATCH_ID, USER_ID))
                    .thenReturn(Optional.of(mp));
            when(matchPlayers.findByIdMatchId(MATCH_ID)).thenReturn(List.of(mp));
            when(users.findAllById(any())).thenReturn(List.of());
            when(matchRounds.findByMatchIdOrderByRoundNumber(MATCH_ID)).thenReturn(List.of(round));
            when(questions.findAllById(any())).thenReturn(List.of(q));
            when(matchAnswers.findByRoundIdIn(any())).thenReturn(List.of());

            MatchDetailResponse response = matchService.getDetail(MATCH_ID, USER_ID);

            assertThat(response.rounds()).hasSize(1);
            assertThat(response.rounds().get(0).correct()).isFalse();
            assertThat(response.rounds().get(0).answerGiven()).isEmpty();
            assertThat(response.rounds().get(0).deviation()).isNull();
        }

        @DisplayName("Respuesta con desviación mapea deviation correctamente")
        @Test
        void respuestaConDesviacion_mapeaDeviationCorrectamente() {
            UUID roundId    = UUID.randomUUID();
            UUID questionId = UUID.randomUUID();

            MatchPlayer mp = player(MATCH_ID, USER_ID, 100, MatchResult.WIN);
            MatchRound round = MatchRound.builder().id(roundId).matchId(MATCH_ID)
                    .questionId(questionId).roundNumber(1).createdAt(Instant.now()).build();
            MatchAnswer answer = MatchAnswer.builder().roundId(roundId).userId(USER_ID)
                    .answerGiven("500").isCorrect(false).deviation(7.5).lifeDelta(-1)
                    .answeredAt(Instant.now()).build();
            Question q = Question.builder().id(questionId).text("¿Cuántos?")
                    .type(QuestionType.NUMERIC).status(QuestionStatus.ACTIVE).build();

            when(matches.findById(MATCH_ID))
                    .thenReturn(Optional.of(match(MATCH_ID, GameMode.PRECISION)));
            when(matchPlayers.findByIdMatchIdAndIdUserId(MATCH_ID, USER_ID))
                    .thenReturn(Optional.of(mp));
            when(matchPlayers.findByIdMatchId(MATCH_ID)).thenReturn(List.of(mp));
            when(users.findAllById(any())).thenReturn(List.of());
            when(matchRounds.findByMatchIdOrderByRoundNumber(MATCH_ID)).thenReturn(List.of(round));
            when(questions.findAllById(any())).thenReturn(List.of(q));
            when(matchAnswers.findByRoundIdIn(any())).thenReturn(List.of(answer));

            MatchDetailResponse response = matchService.getDetail(MATCH_ID, USER_ID);

            assertThat(response.rounds().get(0).deviation()).isEqualTo(7.5);
        }

        @DisplayName("Pregunta ausente en el mapa: questionText vacío")
        @Test
        void preguntaAusente_questionTextVacio() {
            UUID roundId    = UUID.randomUUID();
            UUID questionId = UUID.randomUUID();

            MatchPlayer mp = player(MATCH_ID, USER_ID, 0, MatchResult.LOSS);
            MatchRound round = MatchRound.builder().id(roundId).matchId(MATCH_ID)
                    .questionId(questionId).roundNumber(1).createdAt(Instant.now()).build();

            when(matches.findById(MATCH_ID))
                    .thenReturn(Optional.of(match(MATCH_ID, GameMode.SURVIVAL)));
            when(matchPlayers.findByIdMatchIdAndIdUserId(MATCH_ID, USER_ID))
                    .thenReturn(Optional.of(mp));
            when(matchPlayers.findByIdMatchId(MATCH_ID)).thenReturn(List.of(mp));
            when(users.findAllById(any())).thenReturn(List.of());
            when(matchRounds.findByMatchIdOrderByRoundNumber(MATCH_ID)).thenReturn(List.of(round));
            when(questions.findAllById(any())).thenReturn(List.of());
            when(matchAnswers.findByRoundIdIn(any())).thenReturn(List.of());

            MatchDetailResponse response = matchService.getDetail(MATCH_ID, USER_ID);

            assertThat(response.rounds().get(0).questionText()).isEmpty();
        }

        @DisplayName("Usuario ausente en el mapa: username es 'Unknown'")
        @Test
        void usuarioAusenteEnMap_usernameEsUnknown() {
            UUID roundId = UUID.randomUUID();

            MatchPlayer mp = player(MATCH_ID, USER_ID, 0, MatchResult.LOSS);
            MatchRound round = MatchRound.builder().id(roundId).matchId(MATCH_ID)
                    .questionId(UUID.randomUUID()).roundNumber(1).createdAt(Instant.now()).build();

            when(matches.findById(MATCH_ID))
                    .thenReturn(Optional.of(match(MATCH_ID, GameMode.SURVIVAL)));
            when(matchPlayers.findByIdMatchIdAndIdUserId(MATCH_ID, USER_ID))
                    .thenReturn(Optional.of(mp));
            when(matchPlayers.findByIdMatchId(MATCH_ID)).thenReturn(List.of(mp));
            when(users.findAllById(any())).thenReturn(List.of());
            when(matchRounds.findByMatchIdOrderByRoundNumber(MATCH_ID)).thenReturn(List.of(round));
            when(questions.findAllById(any())).thenReturn(List.of());
            when(matchAnswers.findByRoundIdIn(any())).thenReturn(List.of());

            MatchDetailResponse response = matchService.getDetail(MATCH_ID, USER_ID);

            assertThat(response.players()).hasSize(1);
            assertThat(response.players().get(0).username()).isEqualTo("Unknown");
        }
    }
}
