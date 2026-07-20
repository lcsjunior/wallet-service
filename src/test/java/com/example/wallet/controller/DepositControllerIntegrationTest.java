package com.example.wallet.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.wallet.entity.Wallet;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.support.JsonMocks;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class DepositControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private WalletRepository walletRepository;

  private UUID walletId;

  @BeforeEach
  void setUp() {
    Wallet wallet = Wallet.of(UUID.randomUUID());
    walletRepository.saveAndFlush(wallet);
    walletId = wallet.getId();
  }

  @Test
  @DisplayName("Deve depositar valor válido e retornar 204")
  void shouldDepositWhenAmountIsValid() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + walletId + "/deposits")
                .header("Correlation-Id", "dep-1")
                .contentType(APPLICATION_JSON)
                .content("{\"amount\":\"100.00\"}"))
        .andExpect(status().isNoContent());

    Wallet wallet = walletRepository.findById(walletId).orElseThrow();
    assertThat(wallet.getBalance()).isEqualByComparingTo("100.00");
  }

  @Test
  @DisplayName("Deve rejeitar depósito com valor zero ou negativo")
  void shouldRejectDepositWhenAmountIsZeroOrNegative() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/v1/wallets/" + walletId + "/deposits")
                    .header("Correlation-Id", "dep-2")
                    .contentType(APPLICATION_JSON)
                    .content("{\"amount\":\"-10.00\"}"))
            .andExpect(status().isBadRequest())
            .andReturn();

    String expected =
        JsonMocks.load(
            "common/validation-error.json",
            Map.of(
                "instance",
                "/v1/wallets/" + walletId + "/deposits",
                "errors",
                "[{\"field\":\"amount\",\"message\":\"Amount must be greater than zero\"}]"));
    JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), true);
  }

  @Test
  @DisplayName("Deve rejeitar depósito com mais de duas casas decimais")
  void shouldRejectDepositWhenAmountHasMoreThanTwoDecimalPlaces() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/v1/wallets/" + walletId + "/deposits")
                    .header("Correlation-Id", "dep-3")
                    .contentType(APPLICATION_JSON)
                    .content("{\"amount\":\"0.015\"}"))
            .andExpect(status().isBadRequest())
            .andReturn();

    String expected =
        JsonMocks.load(
            "common/validation-error.json",
            Map.of(
                "instance",
                "/v1/wallets/" + walletId + "/deposits",
                "errors",
                "[{\"field\":\"amount\",\"message\":\"Amount must not have more than 2 decimal"
                    + " places\"}]"));
    JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), true);
  }

  @Test
  @DisplayName("Deve retornar 404 quando a carteira não existe")
  void shouldReturnNotFoundWhenWalletDoesNotExist() throws Exception {
    UUID missingWalletId = UUID.randomUUID();

    MvcResult result =
        mockMvc
            .perform(
                post("/v1/wallets/" + missingWalletId + "/deposits")
                    .header("Correlation-Id", "dep-4")
                    .contentType(APPLICATION_JSON)
                    .content("{\"amount\":\"10.00\"}"))
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
                "/v1/wallets/" + missingWalletId + "/deposits"));
    JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), true);
  }

  @Test
  @DisplayName("Não deve duplicar o efeito ao repetir o mesmo Correlation-Id")
  void shouldNotDuplicateEffectWhenCorrelationIdIsRetried() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + walletId + "/deposits")
                .header("Correlation-Id", "dep-5")
                .contentType(APPLICATION_JSON)
                .content("{\"amount\":\"100.00\"}"))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            post("/v1/wallets/" + walletId + "/deposits")
                .header("Correlation-Id", "dep-5")
                .contentType(APPLICATION_JSON)
                .content("{\"amount\":\"100.00\"}"))
        .andExpect(status().isNoContent());

    Wallet wallet = walletRepository.findById(walletId).orElseThrow();
    assertThat(wallet.getBalance()).isEqualByComparingTo("100.00");
  }

  @Test
  @DisplayName("Deve retornar 409 quando o Correlation-Id é reutilizado com valor diferente")
  void shouldReturnConflictWhenCorrelationIdIsReusedWithDifferentAmount() throws Exception {
    mockMvc
        .perform(
            post("/v1/wallets/" + walletId + "/deposits")
                .header("Correlation-Id", "dep-6")
                .contentType(APPLICATION_JSON)
                .content("{\"amount\":\"100.00\"}"))
        .andExpect(status().isNoContent());

    MvcResult result =
        mockMvc
            .perform(
                post("/v1/wallets/" + walletId + "/deposits")
                    .header("Correlation-Id", "dep-6")
                    .contentType(APPLICATION_JSON)
                    .content("{\"amount\":\"999.00\"}"))
            .andExpect(status().isConflict())
            .andReturn();

    String expected =
        JsonMocks.load(
            "common/error.json",
            Map.of(
                "status", "409",
                "detail", "Correlation-Id was already used with different request parameters",
                "instance", "/v1/wallets/" + walletId + "/deposits"));
    JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), true);
  }
}
