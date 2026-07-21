package com.example.wallet.controller;

import static com.example.wallet.utils.JsonUtils.loadJson;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.json.JsonCompareMode.STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.wallet.AppTests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = "/mock/sql/withdrawal-seed.sql", executionPhase = BEFORE_TEST_METHOD)
class WithdrawalControllerIntegrationTest extends AppTests {

  private static final String WALLET_ID = "35a907a7-9217-4e12-b1f2-5d80f579f9b0";
  private static final String MISSING_WALLET_ID = "55e476d1-f217-4583-a75a-0dd0a548c858";

  @Test
  @DisplayName("Deve sacar valor válido e retornar 204")
  void shouldWithdrawWhenAmountIsValid() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/withdrawals")
                .header("Correlation-Id", "wd-1")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/withdrawal/valid-amount.json")))
        .andExpect(status().isNoContent());

    expectBalance(WALLET_ID, "response/withdrawal/balance-after-withdrawal.json");
  }

  @Test
  @DisplayName("Deve retornar 422 quando o saldo é insuficiente")
  void shouldRejectInsufficientBalance() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/withdrawals")
                .header("Correlation-Id", "wd-2")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/withdrawal/insufficient-amount.json")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            content().json(loadJson("response/withdrawal/insufficient-balance.json"), STRICT));

    expectBalance(WALLET_ID, "response/withdrawal/balance-unchanged.json");
  }

  @Test
  @DisplayName("Deve rejeitar saque com valor inválido")
  void shouldRejectInvalidAmount() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/withdrawals")
                .header("Correlation-Id", "wd-3")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/withdrawal/non-positive-amount.json")))
        .andExpect(status().isBadRequest())
        .andExpect(
            content().json(loadJson("response/withdrawal/reject-invalid-amount.json"), STRICT));
  }

  @Test
  @DisplayName("Deve retornar 404 quando a carteira não existe")
  void shouldReturnNotFoundForMissingWallet() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + MISSING_WALLET_ID + "/withdrawals")
                .header("Correlation-Id", "wd-4")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/withdrawal/valid-amount.json")))
        .andExpect(status().isNotFound())
        .andExpect(content().json(loadJson("response/withdrawal/wallet-not-found.json"), STRICT));
  }

  @Test
  @DisplayName("Não deve duplicar o efeito ao repetir o mesmo Correlation-Id")
  void shouldReplayRetriedCorrelationId() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/withdrawals")
                .header("Correlation-Id", "wd-5")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/withdrawal/valid-amount.json")))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/withdrawals")
                .header("Correlation-Id", "wd-5")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/withdrawal/valid-amount.json")))
        .andExpect(status().isNoContent());

    expectBalance(WALLET_ID, "response/withdrawal/balance-after-withdrawal.json");
  }
}
