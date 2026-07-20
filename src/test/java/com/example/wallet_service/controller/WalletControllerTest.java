package com.example.wallet_service.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.wallet_service.dto.BalanceResponse;
import com.example.wallet_service.dto.WalletResponse;
import com.example.wallet_service.service.WalletService;
import com.example.wallet_service.service.exception.InvalidAsOfException;
import com.example.wallet_service.service.exception.WalletAlreadyExistsException;
import com.example.wallet_service.service.exception.WalletNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WalletController.class)
class WalletControllerTest {

    private static final UUID WALLET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant CREATED_AT = Instant.parse("2026-07-14T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    @Test
    @DisplayName("Deve criar carteira com saldo zero quando o usuário ainda não possui uma")
    void shouldCreateWalletWhenUserHasNone() throws Exception {
        WalletResponse response = new WalletResponse(WALLET_ID, "alice", new BigDecimal("0.00"), CREATED_AT);
        when(walletService.createWallet("alice")).thenReturn(response);

        mockMvc.perform(post("/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"alice\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().json(
                        "{\"walletId\":\"11111111-1111-1111-1111-111111111111\",\"userId\":\"alice\",\"balance\":\"0.00\",\"createdAt\":\"2026-07-14T10:00:00Z\"}",
                        true));
    }

    @Test
    @DisplayName("Deve rejeitar criação de carteira quando o usuário já possui uma")
    void shouldRejectWalletCreationWhenUserAlreadyHasWallet() throws Exception {
        when(walletService.createWallet("alice")).thenThrow(new WalletAlreadyExistsException("alice"));

        mockMvc.perform(post("/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"alice\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().json(
                        "{\"error\":\"WALLET_ALREADY_EXISTS\",\"message\":\"Wallet already exists for user alice\"}",
                        true));
    }

    @Test
    @DisplayName("Deve retornar o saldo atual quando a carteira existe")
    void shouldReturnCurrentBalanceWhenWalletExists() throws Exception {
        BalanceResponse response = new BalanceResponse("alice", new BigDecimal("50.00"), CREATED_AT);
        when(walletService.getCurrentBalance("alice")).thenReturn(response);

        mockMvc.perform(get("/wallets/alice/balance"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"userId\":\"alice\",\"balance\":\"50.00\",\"asOf\":\"2026-07-14T10:00:00Z\"}", true));
    }

    @Test
    @DisplayName("Deve retornar 404 ao consultar saldo de usuário sem carteira")
    void shouldReturnNotFoundWhenQueryingBalanceOfUserWithoutWallet() throws Exception {
        when(walletService.getCurrentBalance("ghost")).thenThrow(new WalletNotFoundException("ghost"));

        mockMvc.perform(get("/wallets/ghost/balance"))
                .andExpect(status().isNotFound())
                .andExpect(content().json(
                        "{\"error\":\"WALLET_NOT_FOUND\",\"message\":\"Wallet not found for user ghost\"}", true));
    }

    @Test
    @DisplayName("Deve retornar o saldo histórico no instante informado")
    void shouldReturnHistoricalBalanceAtGivenInstant() throws Exception {
        Instant asOf = Instant.parse("2026-07-14T10:05:00Z");
        BalanceResponse response = new BalanceResponse("alice", new BigDecimal("100.00"), asOf);
        when(walletService.getBalanceAsOf("alice", asOf)).thenReturn(response);

        mockMvc.perform(get("/wallets/alice/balance").param("asOf", "2026-07-14T10:05:00Z"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"userId\":\"alice\",\"balance\":\"100.00\",\"asOf\":\"2026-07-14T10:05:00Z\"}", true));
    }

    @Test
    @DisplayName("Deve retornar saldo zero para instante anterior à criação da carteira")
    void shouldReturnZeroBalanceForInstantBeforeWalletCreation() throws Exception {
        Instant asOf = Instant.parse("2020-01-01T00:00:00Z");
        BalanceResponse response = new BalanceResponse("alice", new BigDecimal("0.00"), asOf);
        when(walletService.getBalanceAsOf("alice", asOf)).thenReturn(response);

        mockMvc.perform(get("/wallets/alice/balance").param("asOf", "2020-01-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"userId\":\"alice\",\"balance\":\"0.00\",\"asOf\":\"2020-01-01T00:00:00Z\"}", true));
    }

    @Test
    @DisplayName("Deve retornar 400 quando o instante consultado é futuro")
    void shouldReturnBadRequestWhenAsOfIsInTheFuture() throws Exception {
        Instant asOf = Instant.parse("2099-01-01T00:00:00Z");
        when(walletService.getBalanceAsOf("alice", asOf))
                .thenThrow(new InvalidAsOfException("asOf must not be in the future: " + asOf));

        mockMvc.perform(get("/wallets/alice/balance").param("asOf", "2099-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(
                        "{\"error\":\"INVALID_AS_OF\",\"message\":\"asOf must not be in the future: 2099-01-01T00:00:00Z\"}",
                        true));
    }
}
