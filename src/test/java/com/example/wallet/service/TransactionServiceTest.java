package com.example.wallet.service;

import static com.example.wallet.constants.Messages.INSUFFICIENT_BALANCE;
import static com.example.wallet.constants.Messages.SAME_WALLET_TRANSFER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import com.example.wallet.entity.Wallet;
import com.example.wallet.entity.WalletTransaction;
import com.example.wallet.exception.ServiceException;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.repository.WalletTransactionRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

  private static final UUID USER_ID = UUID.fromString("1a1b3c93-6d2f-4f7e-9a41-6c0f2a1d8e33");

  private static final UUID WALLET_ID = UUID.fromString("2c9a1d4b-8e57-4f10-9b3a-5d61e0f27c48");

  private static final UUID PEER_WALLET_ID =
      UUID.fromString("9f4e6a02-7c13-4b8d-a5e6-31c7b90d4f52");

  private static final UUID IDEMPOTENCY_KEY =
      UUID.fromString("4d8b7e15-2a90-4c63-8f21-7b0e5c9a3d16");

  @Mock private WalletRepository walletRepository;

  @Mock private WalletTransactionRepository walletTransactionRepository;

  @InjectMocks private TransactionService transactionService;

  @Nested
  @DisplayName("deposit")
  class Deposit {

    @Test
    @DisplayName("Deve creditar a carteira e registrar o lançamento quando o depósito é novo")
    void shouldCreditWalletWhenDepositIsNew() {
      var wallet = walletWith("100.00");
      when(walletRepository.findWallet(WALLET_ID)).thenReturn(wallet);

      transactionService.deposit(WALLET_ID, new BigDecimal("75.00"), IDEMPOTENCY_KEY);

      assertThat(wallet.getBalance()).isEqualByComparingTo("175.00");
      verify(walletRepository).findWallet(WALLET_ID);
      verify(walletRepository).save(wallet);
      verify(walletTransactionRepository).saveAndFlush(any(WalletTransaction.class));
    }
  }

  @Nested
  @DisplayName("withdraw")
  class Withdraw {

    @Test
    @DisplayName("Deve debitar a carteira e registrar o lançamento quando o saldo é suficiente")
    void shouldDebitWalletWhenBalanceIsSufficient() {
      var wallet = walletWith("100.00");
      when(walletRepository.findWallet(WALLET_ID)).thenReturn(wallet);

      transactionService.withdraw(WALLET_ID, new BigDecimal("75.00"), IDEMPOTENCY_KEY);

      assertThat(wallet.getBalance()).isEqualByComparingTo("25.00");
      verify(walletRepository).findWallet(WALLET_ID);
      verify(walletRepository).save(wallet);
      verify(walletTransactionRepository).saveAndFlush(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("Deve rejeitar com 422 quando o saldo do saque é insuficiente")
    void shouldRejectWhenBalanceIsInsufficient() {
      var wallet = walletWith("10.00");
      when(walletRepository.findWallet(WALLET_ID)).thenReturn(wallet);

      assertThatThrownBy(
              () ->
                  transactionService.withdraw(WALLET_ID, new BigDecimal("75.00"), IDEMPOTENCY_KEY))
          .isInstanceOfSatisfying(
              ServiceException.class,
              exception -> assertThat(exception.getHttpStatus()).isEqualTo(UNPROCESSABLE_ENTITY))
          .hasMessage(INSUFFICIENT_BALANCE);

      assertThat(wallet.getBalance()).isEqualByComparingTo("10.00");
      verify(walletRepository).findWallet(WALLET_ID);
      verify(walletRepository, never()).save(any(Wallet.class));
      verifyNoInteractions(walletTransactionRepository);
    }
  }

  @Nested
  @DisplayName("transfer")
  class Transfer {

    @Test
    @DisplayName("Deve mover o saldo e registrar as duas pernas quando as carteiras são distintas")
    void shouldMoveBalanceWhenWalletsAreDistinct() {
      var fromWallet = walletWith("100.00");
      var toWallet = walletWith("0.00");
      when(walletRepository.findWallet(WALLET_ID)).thenReturn(fromWallet);
      when(walletRepository.findWallet(PEER_WALLET_ID)).thenReturn(toWallet);

      transactionService.transfer(
          WALLET_ID, PEER_WALLET_ID, new BigDecimal("75.00"), IDEMPOTENCY_KEY);

      assertThat(fromWallet.getBalance()).isEqualByComparingTo("25.00");
      assertThat(toWallet.getBalance()).isEqualByComparingTo("75.00");
      verify(walletRepository).findWallet(WALLET_ID);
      verify(walletRepository).findWallet(PEER_WALLET_ID);
      verify(walletRepository).save(fromWallet);
      verify(walletRepository).save(toWallet);
      verify(walletTransactionRepository, times(2)).saveAndFlush(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("Deve rejeitar com 400 quando origem e destino são a mesma carteira")
    void shouldRejectWhenWalletsAreTheSame() {
      assertThatThrownBy(
              () ->
                  transactionService.transfer(
                      WALLET_ID, WALLET_ID, new BigDecimal("75.00"), IDEMPOTENCY_KEY))
          .isInstanceOfSatisfying(
              ServiceException.class,
              exception -> assertThat(exception.getHttpStatus()).isEqualTo(BAD_REQUEST))
          .hasMessage(SAME_WALLET_TRANSFER);

      verifyNoInteractions(walletRepository, walletTransactionRepository);
    }

    @Test
    @DisplayName("Deve rejeitar com 422 quando o saldo da origem é insuficiente")
    void shouldRejectWhenBalanceIsInsufficient() {
      var fromWallet = walletWith("10.00");
      var toWallet = walletWith("0.00");
      when(walletRepository.findWallet(WALLET_ID)).thenReturn(fromWallet);
      when(walletRepository.findWallet(PEER_WALLET_ID)).thenReturn(toWallet);

      assertThatThrownBy(
              () ->
                  transactionService.transfer(
                      WALLET_ID, PEER_WALLET_ID, new BigDecimal("75.00"), IDEMPOTENCY_KEY))
          .isInstanceOfSatisfying(
              ServiceException.class,
              exception -> assertThat(exception.getHttpStatus()).isEqualTo(UNPROCESSABLE_ENTITY))
          .hasMessage(INSUFFICIENT_BALANCE);

      assertThat(fromWallet.getBalance()).isEqualByComparingTo("10.00");
      assertThat(toWallet.getBalance()).isEqualByComparingTo("0.00");
      verify(walletRepository).findWallet(WALLET_ID);
      verify(walletRepository).findWallet(PEER_WALLET_ID);
      verify(walletRepository, never()).save(any(Wallet.class));
      verifyNoInteractions(walletTransactionRepository);
    }
  }

  private static Wallet walletWith(String balance) {
    var wallet = Wallet.of(USER_ID, IDEMPOTENCY_KEY);
    wallet.credit(new BigDecimal(balance));
    return wallet;
  }
}
