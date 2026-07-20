package com.example.wallet_service.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.wallet_service.dto.TransferResponse;
import com.example.wallet_service.service.TransactionService;
import com.example.wallet_service.service.exception.InsufficientBalanceException;
import com.example.wallet_service.service.exception.SameWalletTransferException;
import com.example.wallet_service.service.exception.WalletNotFoundException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransferController.class)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    @DisplayName("Deve transferir valor válido debitando a origem e creditando o destino")
    void shouldTransferWhenAmountIsValidAndBalanceIsSufficient() throws Exception {
        when(transactionService.transfer("alice", "bob", new BigDecimal("75.00"), "tx-1"))
                .thenReturn(new TransferResponse("alice", new BigDecimal("125.00"), "bob", new BigDecimal("75.00"),
                        new BigDecimal("75.00"), "tx-1"));

        mockMvc.perform(post("/transfers")
                        .header("Correlation-Id", "tx-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromUserId\":\"alice\",\"toUserId\":\"bob\",\"amount\":\"75.00\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"fromUserId\":\"alice\",\"fromBalance\":\"125.00\",\"toUserId\":\"bob\",\"toBalance\":\"75.00\",\"amount\":\"75.00\",\"correlationId\":\"tx-1\"}",
                        true));
    }

    @Test
    @DisplayName("Deve retornar 422 quando o saldo de origem é insuficiente")
    void shouldReturnUnprocessableEntityWhenSourceBalanceIsInsufficient() throws Exception {
        when(transactionService.transfer("alice", "bob", new BigDecimal("1000.00"), "tx-2"))
                .thenThrow(new InsufficientBalanceException("alice"));

        mockMvc.perform(post("/transfers")
                        .header("Correlation-Id", "tx-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromUserId\":\"alice\",\"toUserId\":\"bob\",\"amount\":\"1000.00\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().json(
                        "{\"error\":\"INSUFFICIENT_BALANCE\",\"message\":\"Wallet alice has insufficient balance for this operation\"}",
                        true));
    }

    @Test
    @DisplayName("Deve retornar 404 quando a carteira de destino não existe")
    void shouldReturnNotFoundWhenDestinationWalletDoesNotExist() throws Exception {
        when(transactionService.transfer("alice", "ghost", new BigDecimal("10.00"), "tx-3"))
                .thenThrow(new WalletNotFoundException("ghost"));

        mockMvc.perform(post("/transfers")
                        .header("Correlation-Id", "tx-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromUserId\":\"alice\",\"toUserId\":\"ghost\",\"amount\":\"10.00\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().json(
                        "{\"error\":\"WALLET_NOT_FOUND\",\"message\":\"Wallet not found for user ghost\"}", true));
    }

    @Test
    @DisplayName("Deve rejeitar transferência para a própria carteira")
    void shouldRejectTransferToSameWallet() throws Exception {
        when(transactionService.transfer("alice", "alice", new BigDecimal("10.00"), "tx-4"))
                .thenThrow(new SameWalletTransferException("alice"));

        mockMvc.perform(post("/transfers")
                        .header("Correlation-Id", "tx-4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromUserId\":\"alice\",\"toUserId\":\"alice\",\"amount\":\"10.00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(
                        "{\"error\":\"SAME_WALLET_TRANSFER\",\"message\":\"Cannot transfer from wallet alice to itself\"}",
                        true));
    }

    @Test
    @DisplayName("Deve rejeitar transferência com valor inválido")
    void shouldRejectTransferWhenAmountIsInvalid() throws Exception {
        mockMvc.perform(post("/transfers")
                        .header("Correlation-Id", "tx-5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromUserId\":\"alice\",\"toUserId\":\"bob\",\"amount\":\"-5.00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"INVALID_AMOUNT\",\"message\":\"must be greater than 0\"}", true));
    }

    @Test
    @DisplayName("Deve retornar o mesmo resultado ao repetir o mesmo Correlation-Id")
    void shouldReplaySameResultWhenCorrelationIdIsRetried() throws Exception {
        TransferResponse response = new TransferResponse("alice", new BigDecimal("125.00"), "bob",
                new BigDecimal("75.00"), new BigDecimal("75.00"), "tx-1");
        when(transactionService.transfer("alice", "bob", new BigDecimal("75.00"), "tx-1")).thenReturn(response);

        mockMvc.perform(post("/transfers")
                        .header("Correlation-Id", "tx-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromUserId\":\"alice\",\"toUserId\":\"bob\",\"amount\":\"75.00\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/transfers")
                        .header("Correlation-Id", "tx-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromUserId\":\"alice\",\"toUserId\":\"bob\",\"amount\":\"75.00\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"fromUserId\":\"alice\",\"fromBalance\":\"125.00\",\"toUserId\":\"bob\",\"toBalance\":\"75.00\",\"amount\":\"75.00\",\"correlationId\":\"tx-1\"}",
                        true));
    }
}
