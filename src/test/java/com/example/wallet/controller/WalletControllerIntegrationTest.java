package com.example.wallet.controller;

import static com.example.wallet.utils.JsonUtils.loadJson;
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

@Sql(scripts = "/mock/sql/wallet-seed.sql", executionPhase = BEFORE_TEST_METHOD)
class WalletControllerIntegrationTest extends AppTests {

  private static final String WALLET_ID = "8671a4d3-63ec-4129-af91-21f4980ee865";
  private static final String MISSING_WALLET_ID = "55e476d1-f217-4583-a75a-0dd0a548c858";

  @Test
  @DisplayName("Deve criar carteira com saldo zero para um userId informado")
  void shouldCreateWalletWithZeroBalance() throws Exception {
    var response =
        mockMvc
            .perform(
                post("/v1/wallets")
                    .contentType(APPLICATION_JSON)
                    .content(loadJson("request/wallet/create-wallet.json")))
            .andExpect(status().isCreated())
            .andExpect(content().json(loadJson("response/wallet/create-response.json"), LENIENT))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String createdWalletId = JsonPath.parse(response).read("$.walletId");
    expectBalance(createdWalletId, "response/wallet/created-balance.json");
  }

  @Test
  @DisplayName("Deve rejeitar criação de carteira quando o userId não é informado")
  void shouldRejectMissingUserId() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets")
                .contentType(APPLICATION_JSON)
                .content(loadJson("request/wallet/missing-userid.json")))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(loadJson("response/wallet/missing-userid.json"), STRICT));
  }

  @Test
  @DisplayName("Deve retornar o saldo atual quando a carteira existe")
  void shouldReturnCurrentBalance() throws Exception {
    expectBalance(WALLET_ID, "response/wallet/balance-response.json");
  }

  @Test
  @DisplayName("Deve retornar 404 ao consultar saldo de uma carteira inexistente")
  void shouldReturnNotFoundForMissingWallet() throws Exception {
    mockMvc
        .perform(get("/v1/wallets/" + MISSING_WALLET_ID + "/balance"))
        .andExpect(status().isNotFound())
        .andExpect(content().json(loadJson("response/wallet/balance-not-found.json"), STRICT));
  }
}
