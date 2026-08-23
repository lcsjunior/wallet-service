package com.example.wallet.controller;

import static com.example.wallet.constants.Constants.CORRELATION_ID_HEADER;
import static com.example.wallet.testutils.JsonUtils.emptyJson;
import static com.example.wallet.testutils.JsonUtils.fieldJson;
import static com.example.wallet.testutils.JsonUtils.loadJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.json.JsonCompareMode.STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.wallet.AppTests;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = "/mock/sql/clear-tables.sql", executionPhase = BEFORE_TEST_METHOD)
class WalletControllerIntegrationTest extends AppTests {

  private static final String USER_ID = "c528eb21-fd43-46ac-29ba-17d85f394ec1";

  @Test
  @DisplayName("Deve retornar 201 e criar a carteira com saldo zero quando o userId é válido")
  void shouldCreateWalletWhenUserIdIsValid() throws Exception {
    var response =
        mockMvc
            .perform(
                post("/v1/wallets")
                    .header(CORRELATION_ID_HEADER, "00000000-0000-0000-0000-000000000001")
                    .contentType(APPLICATION_JSON)
                    .content(fieldJson("userId", USER_ID)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.walletId").isNotEmpty())
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(balanceOf(JsonPath.read(response, "$.walletId"))).isEqualByComparingTo("0.00");
  }

  @Test
  @DisplayName("Deve retornar 409 e não duplicar a carteira quando o Correlation-Id é repetido")
  void shouldRejectWhenCorrelationIdIsRepeated() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets")
                .header(CORRELATION_ID_HEADER, "00000000-0000-0000-0000-000000000004")
                .contentType(APPLICATION_JSON)
                .content(fieldJson("userId", USER_ID)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/v1/wallets")
                .header(CORRELATION_ID_HEADER, "00000000-0000-0000-0000-000000000004")
                .contentType(APPLICATION_JSON)
                .content(fieldJson("userId", USER_ID)))
        .andExpect(status().isConflict())
        .andExpect(
            content().json(loadJson("response/wallet/error-correlation-conflict.json"), STRICT));

    assertThat(walletRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("Deve retornar 201 e outra carteira quando o mesmo usuário usa outro Correlation-Id")
  void shouldCreateSecondWalletWhenCorrelationIdDiffers() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets")
                .header(CORRELATION_ID_HEADER, "00000000-0000-0000-0000-000000000005")
                .contentType(APPLICATION_JSON)
                .content(fieldJson("userId", USER_ID)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/v1/wallets")
                .header(CORRELATION_ID_HEADER, "00000000-0000-0000-0000-000000000006")
                .contentType(APPLICATION_JSON)
                .content(fieldJson("userId", USER_ID)))
        .andExpect(status().isCreated());

    assertThat(walletRepository.count()).isEqualTo(2);
  }

  @Test
  @DisplayName("Deve retornar 400 quando o userId não é informado")
  void shouldRejectWhenUserIdIsMissing() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets")
                .header(CORRELATION_ID_HEADER, "00000000-0000-0000-0000-000000000002")
                .contentType(APPLICATION_JSON)
                .content(emptyJson()))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(loadJson("response/wallet/error-missing-userid.json"), STRICT));
  }

  @Test
  @DisplayName("Deve retornar 400 quando o Correlation-Id não é informado")
  void shouldRejectWhenCorrelationIdIsMissing() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets").contentType(APPLICATION_JSON).content(fieldJson("userId", USER_ID)))
        .andExpect(status().isBadRequest())
        .andExpect(
            content().json(loadJson("response/wallet/error-missing-correlation-id.json"), STRICT));
  }
}
