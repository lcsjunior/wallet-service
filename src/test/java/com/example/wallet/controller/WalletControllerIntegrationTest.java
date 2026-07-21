package com.example.wallet.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.wallet.support.JsonMocks;
import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class WalletControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("Deve criar carteira com saldo zero para um userId informado")
  void shouldCreateWalletWithZeroBalance() throws Exception {
    var userId = UUID.randomUUID().toString();

    MvcResult result =
        mockMvc
            .perform(
                post("/v1/wallets")
                    .contentType(APPLICATION_JSON)
                    .content("{\"userId\":\"" + userId + "\"}"))
            .andExpect(status().isCreated())
            .andReturn();

    var actual = result.getResponse().getContentAsString();
    String walletId = JsonPath.parse(actual).read("$.walletId");
    String createdAt = JsonPath.parse(actual).read("$.createdAt");
    String expected =
        JsonMocks.load(
            "wallet/create-response.json",
            Map.of("walletId", walletId, "userId", userId, "createdAt", createdAt));

    JSONAssert.assertEquals(expected, actual, true);
  }

  @Test
  @DisplayName("Deve rejeitar criação de carteira quando o userId não é informado")
  void shouldRejectMissingUserId() throws Exception {
    var expected =
        JsonMocks.load(
            "common/validation-error.json",
            Map.of(
                "instance",
                "/v1/wallets",
                "errors",
                JsonMocks.load("common/errors-userid-missing.json")));

    mockMvc
        .perform(post("/v1/wallets").contentType(APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(expected, true));
  }

  @Test
  @DisplayName("Deve retornar o saldo atual quando a carteira existe")
  void shouldReturnCurrentBalance() throws Exception {
    var userId = UUID.randomUUID().toString();
    MvcResult createResult =
        mockMvc
            .perform(
                post("/v1/wallets")
                    .contentType(APPLICATION_JSON)
                    .content("{\"userId\":\"" + userId + "\"}"))
            .andReturn();
    String walletId =
        JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.walletId");

    var expected =
        JsonMocks.load(
            "wallet/balance-response.json", Map.of("walletId", walletId, "balance", "0.00"));

    mockMvc
        .perform(get("/v1/wallets/" + walletId + "/balance"))
        .andExpect(status().isOk())
        .andExpect(content().json(expected, true));
  }

  @Test
  @DisplayName("Deve retornar 404 ao consultar saldo de uma carteira inexistente")
  void shouldReturnNotFoundForMissingWallet() throws Exception {
    var walletId = UUID.randomUUID();
    var expected =
        JsonMocks.load(
            "common/error.json",
            Map.of(
                "status",
                "404",
                "detail",
                "Wallet not found",
                "instance",
                "/v1/wallets/" + walletId + "/balance"));

    mockMvc
        .perform(get("/v1/wallets/" + walletId + "/balance"))
        .andExpect(status().isNotFound())
        .andExpect(content().json(expected, true));
  }
}
