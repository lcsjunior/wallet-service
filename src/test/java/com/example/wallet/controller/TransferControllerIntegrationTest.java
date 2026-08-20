package com.example.wallet.controller;

import static com.example.wallet.constants.Constants.CORRELATION_ID_HEADER;
import static com.example.wallet.constants.Constants.IDEMPOTENT_REPLAYED_HEADER;
import static com.example.wallet.testutils.JsonUtils.loadJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.json.JsonCompareMode.STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.wallet.AppTests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

@Sql(
    scripts = {"/mock/sql/clear-tables.sql", "/mock/sql/transfer-seed.sql"},
    executionPhase = BEFORE_TEST_METHOD)
class TransferControllerIntegrationTest extends AppTests {

  private static final String FROM_WALLET_ID = "7bbda0fe-87ca-42a5-81df-2679d05f4b14";
  private static final String TO_WALLET_ID = "e79b9f63-59d1-4ede-a766-e6e68d53161d";

  @Test
  @DisplayName("Deve retornar 204 movendo o saldo entre as carteiras quando o valor é válido")
  void shouldMoveBalanceWhenAmountIsValid() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header(CORRELATION_ID_HEADER, "id-1")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/transfer/transfer-amount-75.json")))
        .andExpect(status().isNoContent())
        .andExpect(header().string(IDEMPOTENT_REPLAYED_HEADER, "false"));

    assertThat(balanceOf(FROM_WALLET_ID)).isEqualByComparingTo("25.00");
    assertThat(balanceOf(TO_WALLET_ID)).isEqualByComparingTo("75.00");
  }

  @Test
  @DisplayName("Deve retornar 422 quando o saldo da origem é insuficiente")
  void shouldRejectWhenBalanceIsInsufficient() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header(CORRELATION_ID_HEADER, "id-2")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/transfer/transfer-amount-1000.json")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().json(loadJson("response/transfer/error-insufficient.json"), STRICT));

    assertThat(balanceOf(FROM_WALLET_ID)).isEqualByComparingTo("100.00");
    assertThat(balanceOf(TO_WALLET_ID)).isEqualByComparingTo("0.00");
  }

  @Test
  @DisplayName("Deve retornar 404 quando a carteira de origem não existe")
  void shouldRejectWhenFromWalletDoesNotExist() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header(CORRELATION_ID_HEADER, "id-3")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/transfer/transfer-unknown-from-wallet.json")))
        .andExpect(status().isNotFound())
        .andExpect(
            content().json(loadJson("response/transfer/error-wallet-not-found.json"), STRICT));

    assertThat(balanceOf(TO_WALLET_ID)).isEqualByComparingTo("0.00");
  }

  @Test
  @DisplayName("Deve retornar 404 quando a carteira de destino não existe")
  void shouldRejectWhenToWalletDoesNotExist() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header(CORRELATION_ID_HEADER, "id-4")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/transfer/transfer-unknown-to-wallet.json")))
        .andExpect(status().isNotFound())
        .andExpect(
            content().json(loadJson("response/transfer/error-wallet-not-found.json"), STRICT));

    assertThat(balanceOf(FROM_WALLET_ID)).isEqualByComparingTo("100.00");
  }

  @Test
  @DisplayName("Deve retornar 400 quando origem e destino são a mesma carteira")
  void shouldRejectWhenWalletsAreTheSame() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header(CORRELATION_ID_HEADER, "id-5")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/transfer/transfer-same-wallet.json")))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(loadJson("response/transfer/error-same-wallet.json"), STRICT));

    assertThat(balanceOf(FROM_WALLET_ID)).isEqualByComparingTo("100.00");
  }

  @Test
  @DisplayName("Deve retornar 400 quando o valor é zero ou negativo")
  void shouldRejectWhenAmountIsNotPositive() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header(CORRELATION_ID_HEADER, "id-6")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/transfer/transfer-amount-negative.json")))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(loadJson("response/transfer/error-non-positive.json"), STRICT));

    assertThat(balanceOf(FROM_WALLET_ID)).isEqualByComparingTo("100.00");
    assertThat(balanceOf(TO_WALLET_ID)).isEqualByComparingTo("0.00");
  }

  @Test
  @DisplayName("Deve retornar 204 sem mover o saldo de novo quando o Correlation-Id se repete")
  void shouldSkipWhenCorrelationIdRepeats() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header(CORRELATION_ID_HEADER, "id-7")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/transfer/transfer-amount-75.json")))
        .andExpect(status().isNoContent())
        .andExpect(header().string(IDEMPOTENT_REPLAYED_HEADER, "false"));

    mockMvc
        .perform(
            post("/v1/transfers")
                .header(CORRELATION_ID_HEADER, "id-7")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/transfer/transfer-amount-75.json")))
        .andExpect(status().isNoContent())
        .andExpect(header().string(IDEMPOTENT_REPLAYED_HEADER, "true"));

    assertThat(balanceOf(FROM_WALLET_ID)).isEqualByComparingTo("25.00");
    assertThat(balanceOf(TO_WALLET_ID)).isEqualByComparingTo("75.00");
  }
}
