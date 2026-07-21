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

@Sql(scripts = "/mock/sql/deposit-seed.sql", executionPhase = BEFORE_TEST_METHOD)
class DepositControllerIntegrationTest extends AppTests {

  private static final String WALLET_ID = "6163fb26-3a06-4080-a987-35c5e5a17297";
  private static final String MISSING_WALLET_ID = "55e476d1-f217-4583-a75a-0dd0a548c858";

  @Test
  @DisplayName("Deve depositar valor válido e retornar 204")
  void shouldDepositWhenAmountIsValid() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/deposits")
                .header("Correlation-Id", "dep-1")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/deposit/amount-valid.json")))
        .andExpect(status().isNoContent());

    expectBalance(WALLET_ID, "response/deposit/balance-after-deposit.json");
  }

  @Test
  @DisplayName("Deve rejeitar depósito com valor zero ou negativo")
  void shouldRejectNonPositiveAmount() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/deposits")
                .header("Correlation-Id", "dep-2")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/deposit/amount-non-positive.json")))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(loadJson("response/deposit/error-non-positive.json"), STRICT));
  }

  @Test
  @DisplayName("Deve rejeitar depósito com mais de duas casas decimais")
  void shouldRejectAmountWithExtraDecimals() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/deposits")
                .header("Correlation-Id", "dep-3")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/deposit/amount-extra-decimals.json")))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(loadJson("response/deposit/error-extra-decimals.json"), STRICT));
  }

  @Test
  @DisplayName("Deve retornar 404 quando a carteira não existe")
  void shouldReturnNotFoundForMissingWallet() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + MISSING_WALLET_ID + "/deposits")
                .header("Correlation-Id", "dep-4")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/deposit/amount-valid.json")))
        .andExpect(status().isNotFound())
        .andExpect(
            content().json(loadJson("response/deposit/error-wallet-not-found.json"), STRICT));
  }

  @Test
  @DisplayName("Não deve duplicar o efeito ao repetir o mesmo Correlation-Id")
  void shouldReplayRetriedCorrelationId() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/deposits")
                .header("Correlation-Id", "dep-5")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/deposit/amount-valid.json")))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/deposits")
                .header("Correlation-Id", "dep-5")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/deposit/amount-valid.json")))
        .andExpect(status().isNoContent());

    expectBalance(WALLET_ID, "response/deposit/balance-after-deposit.json");
  }

  @Test
  @DisplayName("Deve retornar 409 quando o Correlation-Id é reutilizado com valor diferente")
  void shouldConflictOnReusedCorrelationId() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/deposits")
                .header("Correlation-Id", "dep-6")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/deposit/amount-valid.json")))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/deposits")
                .header("Correlation-Id", "dep-6")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/deposit/amount-conflicting.json")))
        .andExpect(status().isConflict())
        .andExpect(
            content().json(loadJson("response/deposit/error-correlation-conflict.json"), STRICT));
  }
}
