package com.example.wallet.controller;

import static com.example.wallet.constants.AppHeader.IDEMPOTENCY_KEY_HEADER;
import static com.example.wallet.testutils.JsonUtils.loadJson;
import static org.assertj.core.api.Assertions.assertThat;
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

@Sql(
    scripts = {"/mock/sql/clear-tables.sql", "/mock/sql/transfer-seed.sql"},
    executionPhase = BEFORE_TEST_METHOD)
class TransferControllerIntegrationTest extends AppTests {

  private static final String FROM_WALLET_ID = "7bbda0fe-87ca-42a5-81df-2679d05f4b14";
  private static final String TO_WALLET_ID = "e79b9f63-59d1-4ede-a766-e6e68d53161d";
  private static final String MISSING_WALLET_ID = "55e476d1-f217-4583-a75a-0dd0a548c858";

  private static String transferJson(String fromWalletId, String toWalletId, String amount) {
    return """
        {"fromWalletId": "%s", "toWalletId": "%s", "amount": "%s"}"""
        .formatted(fromWalletId, toWalletId, amount);
  }

  @Test
  @DisplayName("Deve retornar 204 movendo o saldo entre as carteiras quando o valor é válido")
  void shouldMoveBalanceWhenAmountIsValid() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header(IDEMPOTENCY_KEY_HEADER, "00000000-0000-0000-0000-000000000001")
                .contentType(APPLICATION_JSON)
                .content(transferJson(FROM_WALLET_ID, TO_WALLET_ID, "75.00")))
        .andExpect(status().isNoContent());

    assertThat(balanceOf(FROM_WALLET_ID)).isEqualByComparingTo("25.00");
    assertThat(balanceOf(TO_WALLET_ID)).isEqualByComparingTo("75.00");
  }

  @Test
  @DisplayName("Deve retornar 400 quando o valor é zero ou negativo")
  void shouldRejectWhenAmountIsNotPositive() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header(IDEMPOTENCY_KEY_HEADER, "00000000-0000-0000-0000-000000000002")
                .contentType(APPLICATION_JSON)
                .content(transferJson(FROM_WALLET_ID, TO_WALLET_ID, "-5.00")))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(loadJson("response/transfer-error-non-positive.json"), STRICT));

    assertThat(balanceOf(FROM_WALLET_ID)).isEqualByComparingTo("100.00");
    assertThat(balanceOf(TO_WALLET_ID)).isEqualByComparingTo("0.00");
  }

  @Test
  @DisplayName("Deve retornar 400 quando o valor tem mais de duas casas decimais")
  void shouldRejectWhenAmountHasExtraDecimals() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header(IDEMPOTENCY_KEY_HEADER, "00000000-0000-0000-0000-000000000003")
                .contentType(APPLICATION_JSON)
                .content(transferJson(FROM_WALLET_ID, TO_WALLET_ID, "0.015")))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(loadJson("response/transfer-error-extra-decimals.json"), STRICT));

    assertThat(balanceOf(FROM_WALLET_ID)).isEqualByComparingTo("100.00");
    assertThat(balanceOf(TO_WALLET_ID)).isEqualByComparingTo("0.00");
  }

  @Test
  @DisplayName("Deve retornar 400 quando origem e destino são a mesma carteira")
  void shouldRejectWhenWalletsAreTheSame() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header(IDEMPOTENCY_KEY_HEADER, "00000000-0000-0000-0000-000000000004")
                .contentType(APPLICATION_JSON)
                .content(transferJson(FROM_WALLET_ID, FROM_WALLET_ID, "10.00")))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(loadJson("response/transfer-error-same-wallet.json"), STRICT));

    assertThat(balanceOf(FROM_WALLET_ID)).isEqualByComparingTo("100.00");
  }

  @Test
  @DisplayName("Deve retornar 422 quando o saldo da origem é insuficiente")
  void shouldRejectWhenBalanceIsInsufficient() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header(IDEMPOTENCY_KEY_HEADER, "00000000-0000-0000-0000-000000000005")
                .contentType(APPLICATION_JSON)
                .content(transferJson(FROM_WALLET_ID, TO_WALLET_ID, "1000.00")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().json(loadJson("response/transfer-error-insufficient.json"), STRICT));

    assertThat(balanceOf(FROM_WALLET_ID)).isEqualByComparingTo("100.00");
    assertThat(balanceOf(TO_WALLET_ID)).isEqualByComparingTo("0.00");
  }

  @Test
  @DisplayName("Deve retornar 404 quando a carteira de origem não existe")
  void shouldRejectWhenFromWalletDoesNotExist() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header(IDEMPOTENCY_KEY_HEADER, "00000000-0000-0000-0000-000000000006")
                .contentType(APPLICATION_JSON)
                .content(transferJson(MISSING_WALLET_ID, TO_WALLET_ID, "10.00")))
        .andExpect(status().isNotFound())
        .andExpect(
            content().json(loadJson("response/transfer-error-wallet-not-found.json"), STRICT));

    assertThat(balanceOf(TO_WALLET_ID)).isEqualByComparingTo("0.00");
  }

  @Test
  @DisplayName("Deve retornar 404 quando a carteira de destino não existe")
  void shouldRejectWhenToWalletDoesNotExist() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header(IDEMPOTENCY_KEY_HEADER, "00000000-0000-0000-0000-000000000007")
                .contentType(APPLICATION_JSON)
                .content(transferJson(FROM_WALLET_ID, MISSING_WALLET_ID, "10.00")))
        .andExpect(status().isNotFound())
        .andExpect(
            content().json(loadJson("response/transfer-error-wallet-not-found.json"), STRICT));

    assertThat(balanceOf(FROM_WALLET_ID)).isEqualByComparingTo("100.00");
  }

  @Test
  @DisplayName("Deve retornar 409 sem mover o saldo de novo quando o Idempotency-Key se repete")
  void shouldRejectWhenIdempotencyKeyRepeats() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header(IDEMPOTENCY_KEY_HEADER, "00000000-0000-0000-0000-000000000008")
                .contentType(APPLICATION_JSON)
                .content(transferJson(FROM_WALLET_ID, TO_WALLET_ID, "25.00")))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            post("/v1/transfers")
                .header(IDEMPOTENCY_KEY_HEADER, "00000000-0000-0000-0000-000000000008")
                .contentType(APPLICATION_JSON)
                .content(transferJson(FROM_WALLET_ID, TO_WALLET_ID, "25.00")))
        .andExpect(status().isConflict())
        .andExpect(
            content().json(loadJson("response/transfer-error-idempotency-conflict.json"), STRICT));

    assertThat(balanceOf(FROM_WALLET_ID)).isEqualByComparingTo("75.00");
    assertThat(balanceOf(TO_WALLET_ID)).isEqualByComparingTo("25.00");
  }
}
