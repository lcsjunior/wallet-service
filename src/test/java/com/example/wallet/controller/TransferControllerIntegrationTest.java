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

@Sql(scripts = "/mock/sql/transfer-seed.sql", executionPhase = BEFORE_TEST_METHOD)
class TransferControllerIntegrationTest extends AppTests {

  private static final String FROM_WALLET_ID = "7bbda0fe-87ca-42a5-81df-2679d05f4b14";
  private static final String TO_WALLET_ID = "e79b9f63-59d1-4ede-a766-e6e68d53161d";

  @Test
  @DisplayName("Deve transferir valor válido debitando a origem e creditando o destino")
  void shouldTransferBetweenWallets() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header("Correlation-Id", "tx-1")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/transfer/valid-transfer.json")))
        .andExpect(status().isNoContent());

    expectBalance(FROM_WALLET_ID, "response/transfer/balance-from-after-transfer.json");
    expectBalance(TO_WALLET_ID, "response/transfer/balance-to-after-transfer.json");
  }

  @Test
  @DisplayName("Deve retornar 422 quando o saldo de origem é insuficiente")
  void shouldRejectInsufficientBalance() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header("Correlation-Id", "tx-2")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/transfer/insufficient-amount.json")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().json(loadJson("response/transfer/insufficient-balance.json"), STRICT));
  }

  @Test
  @DisplayName("Deve retornar 404 quando a carteira de destino não existe")
  void shouldReturnNotFoundForMissingWallet() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header("Correlation-Id", "tx-3")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/transfer/missing-wallet.json")))
        .andExpect(status().isNotFound())
        .andExpect(content().json(loadJson("response/transfer/wallet-not-found.json"), STRICT));
  }

  @Test
  @DisplayName("Deve rejeitar transferência para a própria carteira")
  void shouldRejectTransferToSameWallet() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header("Correlation-Id", "tx-4")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/transfer/same-wallet.json")))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(loadJson("response/transfer/same-wallet.json"), STRICT));
  }

  @Test
  @DisplayName("Deve rejeitar transferência com valor inválido")
  void shouldRejectInvalidAmount() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header("Correlation-Id", "tx-5")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/transfer/non-positive-amount.json")))
        .andExpect(status().isBadRequest())
        .andExpect(
            content().json(loadJson("response/transfer/reject-invalid-amount.json"), STRICT));
  }

  @Test
  @DisplayName("Não deve duplicar o efeito ao repetir o mesmo Correlation-Id")
  void shouldReplayRetriedCorrelationId() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header("Correlation-Id", "tx-6")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/transfer/valid-transfer.json")))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            post("/v1/transfers")
                .header("Correlation-Id", "tx-6")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/transfer/valid-transfer.json")))
        .andExpect(status().isNoContent());

    expectBalance(FROM_WALLET_ID, "response/transfer/balance-from-after-transfer.json");
  }
}
