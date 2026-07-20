package com.example.wallet_service.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.wallet_service.dto.TransactionResponse;
import com.example.wallet_service.service.TransactionService;
import com.example.wallet_service.service.exception.InsufficientBalanceException;
import com.example.wallet_service.service.exception.WalletNotFoundException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WithdrawalController.class)
class WithdrawalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    @DisplayName("Deve sacar valor válido e retornar o novo saldo")
    void shouldWithdrawWhenAmountIsValid() throws Exception {
        when(transactionService.withdraw("alice", new BigDecimal("30.00"), "wd-1"))
                .thenReturn(new TransactionResponse("alice", new BigDecimal("70.00"), new BigDecimal("30.00"), "wd-1"));

        mockMvc.perform(post("/wallets/alice/withdrawals")
                        .header("Correlation-Id", "wd-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"30.00\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"userId\":\"alice\",\"balance\":\"70.00\",\"amount\":\"30.00\",\"correlationId\":\"wd-1\"}",
                        true));
    }

    @Test
    @DisplayName("Deve retornar 422 quando o saldo é insuficiente")
    void shouldReturnUnprocessableEntityWhenBalanceIsInsufficient() throws Exception {
        when(transactionService.withdraw("alice", new BigDecimal("1000.00"), "wd-2"))
                .thenThrow(new InsufficientBalanceException("alice"));

        mockMvc.perform(post("/wallets/alice/withdrawals")
                        .header("Correlation-Id", "wd-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"1000.00\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().json(
                        "{\"error\":\"INSUFFICIENT_BALANCE\",\"message\":\"Wallet alice has insufficient balance for this operation\"}",
                        true));
    }

    @Test
    @DisplayName("Deve rejeitar saque com valor inválido")
    void shouldRejectWithdrawalWhenAmountIsInvalid() throws Exception {
        mockMvc.perform(post("/wallets/alice/withdrawals")
                        .header("Correlation-Id", "wd-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"0\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"INVALID_AMOUNT\",\"message\":\"must be greater than 0\"}", true));
    }

    @Test
    @DisplayName("Deve retornar 404 quando a carteira não existe")
    void shouldReturnNotFoundWhenWalletDoesNotExist() throws Exception {
        when(transactionService.withdraw("ghost", new BigDecimal("10.00"), "wd-4"))
                .thenThrow(new WalletNotFoundException("ghost"));

        mockMvc.perform(post("/wallets/ghost/withdrawals")
                        .header("Correlation-Id", "wd-4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"10.00\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().json(
                        "{\"error\":\"WALLET_NOT_FOUND\",\"message\":\"Wallet not found for user ghost\"}", true));
    }

    @Test
    @DisplayName("Deve retornar o mesmo resultado ao repetir o mesmo Correlation-Id")
    void shouldReplaySameResultWhenCorrelationIdIsRetried() throws Exception {
        TransactionResponse response = new TransactionResponse("alice", new BigDecimal("70.00"), new BigDecimal("30.00"), "wd-1");
        when(transactionService.withdraw("alice", new BigDecimal("30.00"), "wd-1")).thenReturn(response);

        mockMvc.perform(post("/wallets/alice/withdrawals")
                        .header("Correlation-Id", "wd-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"30.00\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/wallets/alice/withdrawals")
                        .header("Correlation-Id", "wd-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"30.00\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"userId\":\"alice\",\"balance\":\"70.00\",\"amount\":\"30.00\",\"correlationId\":\"wd-1\"}",
                        true));
    }
}
