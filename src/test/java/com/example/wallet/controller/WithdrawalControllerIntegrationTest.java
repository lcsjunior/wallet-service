package com.example.wallet.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.wallet.entity.Wallet;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.support.JsonMocks;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WithdrawalControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private WalletRepository walletRepository;

  private UUID walletId;

  @BeforeEach
  void setUp() {
    var wallet = Wallet.of(UUID.randomUUID());
    wallet.credit(new BigDecimal("100.00"));
    walletRepository.saveAndFlush(wallet);
    walletId = wallet.getId();
  }

  @Test
  @DisplayName("Deve sacar valor válido e retornar 204")
  void shouldWithdrawWhenAmountIsValid() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + walletId + "/withdrawals")
                .header("Correlation-Id", "wd-1")
                .contentType(APPLICATION_JSON)
                .content("{\"amount\":\"30.00\"}"))
        .andExpect(status().isNoContent());

    var wallet = walletRepository.findById(walletId).orElseThrow();
    assertThat(wallet.getBalance()).isEqualByComparingTo("70.00");
  }

  @Test
  @DisplayName("Deve retornar 422 quando o saldo é insuficiente")
  void shouldRejectInsufficientBalance() throws Exception {
    var expected =
        JsonMocks.load(
            "common/error.json",
            Map.of(
                "status",
                "422",
                "detail",
                "Wallet has insufficient balance for this operation",
                "instance",
                "/v1/wallets/" + walletId + "/withdrawals"));

    mockMvc
        .perform(
            post("/v1/wallets/" + walletId + "/withdrawals")
                .header("Correlation-Id", "wd-2")
                .contentType(APPLICATION_JSON)
                .content("{\"amount\":\"1000.00\"}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().json(expected, true));

    var wallet = walletRepository.findById(walletId).orElseThrow();
    assertThat(wallet.getBalance()).isEqualByComparingTo("100.00");
  }

  @Test
  @DisplayName("Deve rejeitar saque com valor inválido")
  void shouldRejectInvalidAmount() throws Exception {
    var expected =
        JsonMocks.load(
            "common/validation-error.json",
            Map.of(
                "instance",
                "/v1/wallets/" + walletId + "/withdrawals",
                "errors",
                JsonMocks.load("common/errors-amount-positive.json")));

    mockMvc
        .perform(
            post("/v1/wallets/" + walletId + "/withdrawals")
                .header("Correlation-Id", "wd-3")
                .contentType(APPLICATION_JSON)
                .content("{\"amount\":\"0\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(expected, true));
  }

  @Test
  @DisplayName("Deve retornar 404 quando a carteira não existe")
  void shouldReturnNotFoundForMissingWallet() throws Exception {
    var missingWalletId = UUID.randomUUID();
    var expected =
        JsonMocks.load(
            "common/error.json",
            Map.of(
                "status",
                "404",
                "detail",
                "Wallet not found",
                "instance",
                "/v1/wallets/" + missingWalletId + "/withdrawals"));

    mockMvc
        .perform(
            post("/v1/wallets/" + missingWalletId + "/withdrawals")
                .header("Correlation-Id", "wd-4")
                .contentType(APPLICATION_JSON)
                .content("{\"amount\":\"10.00\"}"))
        .andExpect(status().isNotFound())
        .andExpect(content().json(expected, true));
  }

  @Test
  @DisplayName("Não deve duplicar o efeito ao repetir o mesmo Correlation-Id")
  void shouldReplayRetriedCorrelationId() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + walletId + "/withdrawals")
                .header("Correlation-Id", "wd-5")
                .contentType(APPLICATION_JSON)
                .content("{\"amount\":\"30.00\"}"))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            post("/v1/wallets/" + walletId + "/withdrawals")
                .header("Correlation-Id", "wd-5")
                .contentType(APPLICATION_JSON)
                .content("{\"amount\":\"30.00\"}"))
        .andExpect(status().isNoContent());

    var wallet = walletRepository.findById(walletId).orElseThrow();
    assertThat(wallet.getBalance()).isEqualByComparingTo("70.00");
  }
}
