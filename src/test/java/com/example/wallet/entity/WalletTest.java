package com.example.wallet.entity;

import static com.example.wallet.constants.Messages.INSUFFICIENT_BALANCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import com.example.wallet.exception.ServiceException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WalletTest {

  private static final UUID USER_ID = UUID.fromString("1a1b3c93-6d2f-4f7e-9a41-6c0f2a1d8e33");

  private static final UUID IDEMPOTENCY_KEY =
      UUID.fromString("6b2c8f41-5d70-4e19-b83c-9a1f0e5d7c24");

  @Test
  @DisplayName("Deve iniciar com saldo zero na escala 2 quando a carteira é criada")
  void shouldStartWithZeroBalanceWhenWalletIsCreated() {
    assertThat(Wallet.of(USER_ID, IDEMPOTENCY_KEY).getBalance()).isEqualTo(new BigDecimal("0.00"));
  }

  @Test
  @DisplayName("Deve somar ao saldo mantendo a escala 2 quando a carteira é creditada")
  void shouldAddToBalanceWhenWalletIsCredited() {
    var wallet = Wallet.of(USER_ID, IDEMPOTENCY_KEY);

    wallet.credit(new BigDecimal("100.00"));
    wallet.credit(new BigDecimal("75.50"));

    assertThat(wallet.getBalance()).isEqualTo(new BigDecimal("175.50"));
  }

  @Test
  @DisplayName("Deve subtrair do saldo mantendo a escala 2 quando o saldo é suficiente")
  void shouldSubtractFromBalanceWhenBalanceIsSufficient() {
    var wallet = walletWith("100.00");

    wallet.debit(new BigDecimal("75.50"));

    assertThat(wallet.getBalance()).isEqualTo(new BigDecimal("24.50"));
  }

  @Test
  @DisplayName("Deve zerar o saldo quando o débito é igual ao saldo")
  void shouldEmptyBalanceWhenDebitEqualsBalance() {
    var wallet = walletWith("100.00");

    wallet.debit(new BigDecimal("100.00"));

    assertThat(wallet.getBalance()).isEqualTo(new BigDecimal("0.00"));
  }

  @Test
  @DisplayName("Deve rejeitar com 422 preservando o saldo quando o saldo é insuficiente")
  void shouldRejectWhenBalanceIsInsufficient() {
    var wallet = walletWith("10.00");

    assertThatThrownBy(() -> wallet.debit(new BigDecimal("10.01")))
        .isInstanceOfSatisfying(
            ServiceException.class,
            exception -> assertThat(exception.getHttpStatus()).isEqualTo(UNPROCESSABLE_ENTITY))
        .hasMessage(INSUFFICIENT_BALANCE);

    assertThat(wallet.getBalance()).isEqualTo(new BigDecimal("10.00"));
  }

  private static Wallet walletWith(String balance) {
    var wallet = Wallet.of(USER_ID, IDEMPOTENCY_KEY);
    wallet.credit(new BigDecimal(balance));
    return wallet;
  }
}
