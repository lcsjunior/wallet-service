package com.example.wallet.exception;

import static com.example.wallet.constants.Constants.IDEMPOTENCY_KEY_HEADER;
import static com.example.wallet.controller.DepositControllerIntegrationTest.depositJson;
import static com.example.wallet.testutils.JsonUtils.loadJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.json.JsonCompareMode.STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.wallet.AppTests;
import com.example.wallet.entity.Wallet;
import com.example.wallet.repository.WalletTransactionRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;

@Sql(
    scripts = {"/mock/sql/clear-tables.sql", "/mock/sql/optimistic-lock-seed.sql"},
    executionPhase = BEFORE_TEST_METHOD)
class OptimisticLockIntegrationTest extends AppTests {

  private static final String WALLET_ID = "c8f2a41d-0b6e-4d93-9a75-3e1c7f45b208";

  @MockitoSpyBean private WalletTransactionRepository walletTransactionRepository;

  @Test
  @DisplayName("Deve retornar 409 sem alterar o saldo quando o lock otimista falha")
  void shouldRejectWhenOptimisticLockFails() throws Exception {
    doThrow(new ObjectOptimisticLockingFailureException(Wallet.class, UUID.fromString(WALLET_ID)))
        .when(walletTransactionRepository)
        .saveAndFlush(any());

    mockMvc
        .perform(
            post("/v1/wallets/" + WALLET_ID + "/deposits")
                .header(IDEMPOTENCY_KEY_HEADER, "00000000-0000-0000-0000-000000000006")
                .contentType(APPLICATION_JSON)
                .content(depositJson("50.00")))
        .andExpect(status().isConflict())
        .andExpect(
            content().json(loadJson("response/deposit-error-concurrent-update.json"), STRICT));

    assertThat(balanceOf(WALLET_ID)).isEqualByComparingTo("100.00");
  }
}
