package com.example.wallet.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.wallet.entity.Wallet;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.support.JsonMocks;
import java.math.BigDecimal;
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
class TransferControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private WalletRepository walletRepository;

  private UUID fromWalletId;
  private UUID toWalletId;

  @BeforeEach
  void setUp() {
    Wallet fromWallet = Wallet.of(UUID.randomUUID());
    fromWallet.credit(new BigDecimal("100.00"));
    walletRepository.saveAndFlush(fromWallet);
    fromWalletId = fromWallet.getId();

    Wallet toWallet = Wallet.of(UUID.randomUUID());
    walletRepository.saveAndFlush(toWallet);
    toWalletId = toWallet.getId();
  }

  @Test
  @DisplayName("Deve transferir valor válido debitando a origem e creditando o destino")
  void shouldTransferWhenAmountIsValidAndBalanceIsSufficient() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header("Correlation-Id", "tx-1")
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"fromWalletId\":\""
                        + fromWalletId
                        + "\",\"toWalletId\":\""
                        + toWalletId
                        + "\",\"amount\":\"75.00\"}"))
        .andExpect(status().isNoContent());

    assertThat(walletRepository.findById(fromWalletId).orElseThrow().getBalance())
        .isEqualByComparingTo("25.00");
    assertThat(walletRepository.findById(toWalletId).orElseThrow().getBalance())
        .isEqualByComparingTo("75.00");
  }

  @Test
  @DisplayName("Deve retornar 422 quando o saldo de origem é insuficiente")
  void shouldReturnUnprocessableEntityWhenSourceBalanceIsInsufficient() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/v1/transfers")
                    .header("Correlation-Id", "tx-2")
                    .contentType(APPLICATION_JSON)
                    .content(
                        "{\"fromWalletId\":\""
                            + fromWalletId
                            + "\",\"toWalletId\":\""
                            + toWalletId
                            + "\",\"amount\":\"1000.00\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

    String expected =
        JsonMocks.load(
            "common/error.json",
            Map.of(
                "status", "422",
                "detail", "Wallet has insufficient balance for this operation",
                "instance", "/v1/transfers"));
    JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), true);
  }

  @Test
  @DisplayName("Deve retornar 404 quando a carteira de destino não existe")
  void shouldReturnNotFoundWhenDestinationWalletDoesNotExist() throws Exception {
    UUID missingWalletId = UUID.randomUUID();

    MvcResult result =
        mockMvc
            .perform(
                post("/v1/transfers")
                    .header("Correlation-Id", "tx-3")
                    .contentType(APPLICATION_JSON)
                    .content(
                        "{\"fromWalletId\":\""
                            + fromWalletId
                            + "\",\"toWalletId\":\""
                            + missingWalletId
                            + "\",\"amount\":\"10.00\"}"))
            .andExpect(status().isNotFound())
            .andReturn();

    String expected =
        JsonMocks.load(
            "common/error.json",
            Map.of(
                "status", "404",
                "detail", "Wallet not found",
                "instance", "/v1/transfers"));
    JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), true);
  }

  @Test
  @DisplayName("Deve rejeitar transferência para a própria carteira")
  void shouldRejectTransferToSameWallet() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/v1/transfers")
                    .header("Correlation-Id", "tx-4")
                    .contentType(APPLICATION_JSON)
                    .content(
                        "{\"fromWalletId\":\""
                            + fromWalletId
                            + "\",\"toWalletId\":\""
                            + fromWalletId
                            + "\",\"amount\":\"10.00\"}"))
            .andExpect(status().isBadRequest())
            .andReturn();

    String expected =
        JsonMocks.load(
            "common/error.json",
            Map.of(
                "status", "400",
                "detail", "Cannot transfer from a wallet to itself",
                "instance", "/v1/transfers"));
    JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), true);
  }

  @Test
  @DisplayName("Deve rejeitar transferência com valor inválido")
  void shouldRejectTransferWhenAmountIsInvalid() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/v1/transfers")
                    .header("Correlation-Id", "tx-5")
                    .contentType(APPLICATION_JSON)
                    .content(
                        "{\"fromWalletId\":\""
                            + fromWalletId
                            + "\",\"toWalletId\":\""
                            + toWalletId
                            + "\",\"amount\":\"-5.00\"}"))
            .andExpect(status().isBadRequest())
            .andReturn();

    String expected =
        JsonMocks.load(
            "common/validation-error.json",
            Map.of(
                "instance",
                "/v1/transfers",
                "errors",
                "[{\"field\":\"amount\",\"message\":\"Amount must be greater than zero\"}]"));
    JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), true);
  }

  @Test
  @DisplayName("Não deve duplicar o efeito ao repetir o mesmo Correlation-Id")
  void shouldNotDuplicateEffectWhenCorrelationIdIsRetried() throws Exception {
    mockMvc
        .perform(
            post("/v1/transfers")
                .header("Correlation-Id", "tx-6")
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"fromWalletId\":\""
                        + fromWalletId
                        + "\",\"toWalletId\":\""
                        + toWalletId
                        + "\",\"amount\":\"75.00\"}"))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            post("/v1/transfers")
                .header("Correlation-Id", "tx-6")
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"fromWalletId\":\""
                        + fromWalletId
                        + "\",\"toWalletId\":\""
                        + toWalletId
                        + "\",\"amount\":\"75.00\"}"))
        .andExpect(status().isNoContent());

    assertThat(walletRepository.findById(fromWalletId).orElseThrow().getBalance())
        .isEqualByComparingTo("25.00");
  }
}
