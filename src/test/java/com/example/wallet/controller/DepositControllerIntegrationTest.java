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
    scripts = {"/mock/sql/clear-tables.sql", "/mock/sql/deposit-seed.sql"},
    executionPhase = BEFORE_TEST_METHOD)
public class DepositControllerIntegrationTest extends AppTests {

  private static final String WALLET_ID = "6163fb26-3a06-4080-a987-35c5e5a17297";
  private static final String MISSING_WALLET_ID = "55e476d1-f217-4583-a75a-0dd0a548c858";

  public static String depositJson(String amount) {
    return """
        {"amount": "%s"}"""
        .formatted(amount);
  }

  @Test
  @DisplayName("Deve retornar 204 e creditar o saldo quando o valor é válido")
  void shouldCreditBalanceWhenAmountIsValid() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/deposits")
                .header(IDEMPOTENCY_KEY_HEADER, "00000000-0000-0000-0000-000000000001")
                .contentType(APPLICATION_JSON)
                .content(depositJson("100.00")))
        .andExpect(status().isNoContent());

    assertThat(balanceOf(WALLET_ID)).isEqualByComparingTo("100.00");
  }

  @Test
  @DisplayName("Deve retornar 400 quando o valor é zero ou negativo")
  void shouldRejectWhenAmountIsNotPositive() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/deposits")
                .header(IDEMPOTENCY_KEY_HEADER, "00000000-0000-0000-0000-000000000002")
                .contentType(APPLICATION_JSON)
                .content(depositJson("-10.00")))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(loadJson("response/deposit-error-non-positive.json"), STRICT));

    assertThat(balanceOf(WALLET_ID)).isEqualByComparingTo("0.00");
  }

  @Test
  @DisplayName("Deve retornar 400 quando o valor tem mais de duas casas decimais")
  void shouldRejectWhenAmountHasExtraDecimals() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/deposits")
                .header(IDEMPOTENCY_KEY_HEADER, "00000000-0000-0000-0000-000000000003")
                .contentType(APPLICATION_JSON)
                .content(depositJson("0.015")))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(loadJson("response/deposit-error-extra-decimals.json"), STRICT));

    assertThat(balanceOf(WALLET_ID)).isEqualByComparingTo("0.00");
  }

  @Test
  @DisplayName("Deve retornar 404 quando a carteira não existe")
  void shouldRejectWhenWalletDoesNotExist() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + MISSING_WALLET_ID + "/deposits")
                .header(IDEMPOTENCY_KEY_HEADER, "00000000-0000-0000-0000-000000000004")
                .contentType(APPLICATION_JSON)
                .content(depositJson("100.00")))
        .andExpect(status().isNotFound())
        .andExpect(
            content().json(loadJson("response/deposit-error-wallet-not-found.json"), STRICT));

    assertThat(balanceOf(WALLET_ID)).isEqualByComparingTo("0.00");
  }

  @Test
  @DisplayName("Deve retornar 409 sem creditar de novo quando o Idempotency-Key se repete")
  void shouldRejectWhenIdempotencyKeyRepeats() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/deposits")
                .header(IDEMPOTENCY_KEY_HEADER, "00000000-0000-0000-0000-000000000005")
                .contentType(APPLICATION_JSON)
                .content(depositJson("100.00")))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/deposits")
                .header(IDEMPOTENCY_KEY_HEADER, "00000000-0000-0000-0000-000000000005")
                .contentType(APPLICATION_JSON)
                .content(depositJson("100.00")))
        .andExpect(status().isConflict())
        .andExpect(
            content().json(loadJson("response/deposit-error-idempotency-conflict.json"), STRICT));

    assertThat(balanceOf(WALLET_ID)).isEqualByComparingTo("100.00");
  }
}
