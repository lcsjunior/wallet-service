package com.example.wallet.controller;

import static com.example.wallet.constants.Constants.IDEMPOTENCY_KEY_HEADER;
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
    scripts = {"/mock/sql/clear-tables.sql", "/mock/sql/withdrawal-seed.sql"},
    executionPhase = BEFORE_TEST_METHOD)
class WithdrawalControllerIntegrationTest extends AppTests {

  private static final String WALLET_ID = "35a907a7-9217-4e12-b1f2-5d80f579f9b0";
  private static final String MISSING_WALLET_ID = "55e476d1-f217-4583-a75a-0dd0a548c858";

  private static String withdrawalJson(String amount) {
    return """
        {"amount": "%s"}"""
        .formatted(amount);
  }

  @Test
  @DisplayName("Deve retornar 204 e debitar o saldo quando o valor é válido")
  void shouldDebitBalanceWhenAmountIsValid() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/withdrawals")
                .header(IDEMPOTENCY_KEY_HEADER, "00000000-0000-0000-0000-000000000001")
                .contentType(APPLICATION_JSON)
                .content(withdrawalJson("30.00")))
        .andExpect(status().isNoContent());

    assertThat(balanceOf(WALLET_ID)).isEqualByComparingTo("70.00");
  }

  @Test
  @DisplayName("Deve retornar 422 quando o saldo é insuficiente")
  void shouldRejectWhenBalanceIsInsufficient() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/withdrawals")
                .header(IDEMPOTENCY_KEY_HEADER, "00000000-0000-0000-0000-000000000002")
                .contentType(APPLICATION_JSON)
                .content(withdrawalJson("1000.00")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().json(loadJson("response/withdrawal/error-insufficient.json"), STRICT));

    assertThat(balanceOf(WALLET_ID)).isEqualByComparingTo("100.00");
  }

  @Test
  @DisplayName("Deve retornar 400 quando o valor é zero ou negativo")
  void shouldRejectWhenAmountIsNotPositive() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/withdrawals")
                .header(IDEMPOTENCY_KEY_HEADER, "00000000-0000-0000-0000-000000000003")
                .contentType(APPLICATION_JSON)
                .content(withdrawalJson("0")))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(loadJson("response/withdrawal/error-non-positive.json"), STRICT));

    assertThat(balanceOf(WALLET_ID)).isEqualByComparingTo("100.00");
  }

  @Test
  @DisplayName("Deve retornar 404 quando a carteira não existe")
  void shouldRejectWhenWalletDoesNotExist() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + MISSING_WALLET_ID + "/withdrawals")
                .header(IDEMPOTENCY_KEY_HEADER, "00000000-0000-0000-0000-000000000004")
                .contentType(APPLICATION_JSON)
                .content(withdrawalJson("30.00")))
        .andExpect(status().isNotFound())
        .andExpect(
            content().json(loadJson("response/withdrawal/error-wallet-not-found.json"), STRICT));

    assertThat(balanceOf(WALLET_ID)).isEqualByComparingTo("100.00");
  }

  @Test
  @DisplayName("Deve retornar 409 sem debitar de novo quando o Idempotency-Key se repete")
  void shouldRejectWhenIdempotencyKeyRepeats() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/withdrawals")
                .header(IDEMPOTENCY_KEY_HEADER, "00000000-0000-0000-0000-000000000005")
                .contentType(APPLICATION_JSON)
                .content(withdrawalJson("30.00")))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/withdrawals")
                .header(IDEMPOTENCY_KEY_HEADER, "00000000-0000-0000-0000-000000000005")
                .contentType(APPLICATION_JSON)
                .content(withdrawalJson("30.00")))
        .andExpect(status().isConflict())
        .andExpect(
            content()
                .json(loadJson("response/withdrawal/error-idempotency-conflict.json"), STRICT));

    assertThat(balanceOf(WALLET_ID)).isEqualByComparingTo("70.00");
  }
}
