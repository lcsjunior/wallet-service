package com.example.wallet.controller;

import static com.example.wallet.utils.JsonUtils.emptyJson;
import static com.example.wallet.utils.JsonUtils.fieldJson;
import static com.example.wallet.utils.JsonUtils.loadJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.json.JsonCompareMode.LENIENT;
import static org.springframework.test.json.JsonCompareMode.STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.wallet.AppTests;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

@Sql(
    scripts = {"/mock/sql/clear-tables.sql", "/mock/sql/wallet-seed.sql"},
    executionPhase = BEFORE_TEST_METHOD)
class WalletControllerIntegrationTest extends AppTests {

  private static final String WALLET_ID = "8671a4d3-63ec-4129-af91-21f4980ee865";
  private static final String MISSING_WALLET_ID = "55e476d1-f217-4583-a75a-0dd0a548c858";
  private static final String USER_ID = "c528eb21-fd43-46ac-29ba-17d85f394ec1";

  @Test
  @DisplayName("Deve retornar 201 e criar a carteira com saldo zero quando o userId é válido")
  void shouldCreateWalletWhenUserIdIsValid() throws Exception {
    var response =
        mockMvc
            .perform(
                post("/v1/wallets")
                    .contentType(APPLICATION_JSON)
                    .content(fieldJson("userId", USER_ID)))
            .andExpect(status().isCreated())
            .andExpect(content().json(fieldJson("balance", "0.00"), LENIENT))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String createdWalletId = JsonPath.parse(response).read("$.walletId");
    assertThat(balanceOf(createdWalletId)).isEqualByComparingTo("0.00");
  }

  @Test
  @DisplayName("Deve retornar 400 quando o userId não é informado")
  void shouldRejectWhenUserIdIsMissing() throws Exception {
    mockMvc
        .perform(post("/v1/wallets").contentType(APPLICATION_JSON).content(emptyJson()))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(loadJson("response/wallet/error-missing-userid.json"), STRICT));
  }

  @Test
  @DisplayName("Deve retornar 200 com o saldo atual quando a carteira existe")
  void shouldReturnBalanceWhenWalletExists() throws Exception {
    mockMvc
        .perform(get("/v1/wallets/" + WALLET_ID + "/balance"))
        .andExpect(status().isOk())
        .andExpect(content().json(fieldJson("balance", "0.00"), STRICT));
  }

  @Test
  @DisplayName("Deve retornar 404 quando a carteira não existe")
  void shouldRejectWhenWalletDoesNotExist() throws Exception {
    mockMvc
        .perform(get("/v1/wallets/" + MISSING_WALLET_ID + "/balance"))
        .andExpect(status().isNotFound())
        .andExpect(content().json(loadJson("response/wallet/error-wallet-not-found.json"), STRICT));
  }
}
