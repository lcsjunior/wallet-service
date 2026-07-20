package com.example.wallet_service.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.wallet_service.dto.TransactionResponse;
import com.example.wallet_service.service.TransactionService;
import com.example.wallet_service.service.exception.IdempotencyConflictException;
import com.example.wallet_service.service.exception.WalletNotFoundException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DepositController.class)
class DepositControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    @DisplayName("Deve depositar valor válido e retornar o novo saldo")
    void shouldDepositWhenAmountIsValid() throws Exception {
        when(transactionService.deposit("alice", new BigDecimal("100.00"), "dep-1"))
                .thenReturn(new TransactionResponse("alice", new BigDecimal("250.00"), new BigDecimal("100.00"), "dep-1"));

        mockMvc.perform(post("/wallets/alice/deposits")
                        .header("Correlation-Id", "dep-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"100.00\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"userId\":\"alice\",\"balance\":\"250.00\",\"amount\":\"100.00\",\"correlationId\":\"dep-1\"}",
                        true));
    }

    @Test
    @DisplayName("Deve rejeitar depósito com valor zero ou negativo")
    void shouldRejectDepositWhenAmountIsZeroOrNegative() throws Exception {
        mockMvc.perform(post("/wallets/alice/deposits")
                        .header("Correlation-Id", "dep-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"-10.00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"INVALID_AMOUNT\",\"message\":\"must be greater than 0\"}", true));
    }

    @Test
    @DisplayName("Deve rejeitar depósito com mais de duas casas decimais")
    void shouldRejectDepositWhenAmountHasMoreThanTwoDecimalPlaces() throws Exception {
        mockMvc.perform(post("/wallets/alice/deposits")
                        .header("Correlation-Id", "dep-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"0.015\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(
                        "{\"error\":\"INVALID_AMOUNT_SCALE\",\"message\":\"numeric value out of bounds (<17 digits>.<2 digits> expected)\"}",
                        true));
    }

    @Test
    @DisplayName("Deve retornar 404 quando a carteira não existe")
    void shouldReturnNotFoundWhenWalletDoesNotExist() throws Exception {
        when(transactionService.deposit("ghost", new BigDecimal("10.00"), "dep-4"))
                .thenThrow(new WalletNotFoundException("ghost"));

        mockMvc.perform(post("/wallets/ghost/deposits")
                        .header("Correlation-Id", "dep-4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"10.00\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().json(
                        "{\"error\":\"WALLET_NOT_FOUND\",\"message\":\"Wallet not found for user ghost\"}", true));
    }

    @Test
    @DisplayName("Deve retornar o mesmo resultado ao repetir o mesmo Correlation-Id")
    void shouldReplaySameResultWhenCorrelationIdIsRetried() throws Exception {
        TransactionResponse response = new TransactionResponse("alice", new BigDecimal("250.00"), new BigDecimal("100.00"), "dep-1");
        when(transactionService.deposit("alice", new BigDecimal("100.00"), "dep-1")).thenReturn(response);

        mockMvc.perform(post("/wallets/alice/deposits")
                        .header("Correlation-Id", "dep-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"100.00\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/wallets/alice/deposits")
                        .header("Correlation-Id", "dep-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"100.00\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"userId\":\"alice\",\"balance\":\"250.00\",\"amount\":\"100.00\",\"correlationId\":\"dep-1\"}",
                        true));
    }

    @Test
    @DisplayName("Deve retornar 409 quando o Correlation-Id é reutilizado com valor diferente")
    void shouldReturnConflictWhenCorrelationIdIsReusedWithDifferentAmount() throws Exception {
        when(transactionService.deposit("alice", new BigDecimal("999.00"), "dep-1"))
                .thenThrow(new IdempotencyConflictException("dep-1"));

        mockMvc.perform(post("/wallets/alice/deposits")
                        .header("Correlation-Id", "dep-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"999.00\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().json(
                        "{\"error\":\"CORRELATION_ID_CONFLICT\",\"message\":\"Correlation-Id dep-1 was already used with different request parameters\"}",
                        true));
    }
}
