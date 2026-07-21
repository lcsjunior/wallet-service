package com.example.wallet.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
  void shouldRejectWalletCreationWhenUserIdIsMissing() throws Exception {
    mockMvc
        .perform(post("/v1/wallets").contentType(APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Deve retornar o saldo atual quando a carteira existe")
  void shouldReturnCurrentBalanceWhenWalletExists() throws Exception {
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

    MvcResult result =
        mockMvc
            .perform(get("/v1/wallets/" + walletId + "/balance"))
            .andExpect(status().isOk())
            .andReturn();

    String expected =
        JsonMocks.load(
            "wallet/balance-response.json", Map.of("walletId", walletId, "balance", "0.00"));
    JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), true);
  }

  @Test
  @DisplayName("Deve retornar 404 ao consultar saldo de uma carteira inexistente")
  void shouldReturnNotFoundWhenQueryingBalanceOfMissingWallet() throws Exception {
    var walletId = UUID.randomUUID();

    MvcResult result =
        mockMvc
            .perform(get("/v1/wallets/" + walletId + "/balance"))
            .andExpect(status().isNotFound())
            .andReturn();

    String expected =
        JsonMocks.load(
            "common/error.json",
            Map.of(
                "status",
                "404",
                "detail",
                "Wallet not found",
                "instance",
                "/v1/wallets/" + walletId + "/balance"));
    JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), true);
  }
}
