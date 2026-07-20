package com.example.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.wallet.dto.TransactionResponse;
import com.example.wallet.entity.IdempotencyEntry;
import com.example.wallet.entity.OperationType;
import com.example.wallet.entity.Wallet;
import com.example.wallet.repository.IdempotencyEntryRepository;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.repository.WalletTransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

  @Mock private WalletRepository walletRepository;
  @Mock private WalletTransactionRepository walletTransactionRepository;
  @Mock private IdempotencyEntryRepository idempotencyEntryRepository;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private TransactionService transactionService() {
    return new TransactionService(
        walletRepository, walletTransactionRepository, idempotencyEntryRepository, objectMapper);
  }

  @Test
  @DisplayName("should check the idempotency repository before executing a deposit")
  void shouldCheckCacheBeforeDeposit() {
    var walletId = UUID.randomUUID();
    var correlationId = "corr-1";
    when(idempotencyEntryRepository.findById(correlationId)).thenReturn(Optional.empty());
    when(walletRepository.findByIdForUpdate(walletId))
        .thenReturn(Optional.of(Wallet.of(UUID.randomUUID())));

    transactionService().deposit(walletId, new BigDecimal("10.00"), correlationId);

    verify(idempotencyEntryRepository).findById(correlationId);
  }

  @Test
  @DisplayName("should not persist or cache a new idempotency entry when a deposit is replayed")
  void shouldSkipPersistOnReplayedDeposit() throws Exception {
    var walletId = UUID.randomUUID();
    var correlationId = "corr-2";
    var amount = new BigDecimal("10.00");
    var fingerprint = OperationType.DEPOSIT + ":" + walletId + ":" + amount.toPlainString();
    var previousResponse =
        new TransactionResponse(walletId, new BigDecimal("50.00"), amount, correlationId);
    var body = objectMapper.writeValueAsString(previousResponse);
    var cachedEntry = IdempotencyEntry.of(correlationId, OperationType.DEPOSIT, fingerprint, body);
    when(idempotencyEntryRepository.findById(correlationId)).thenReturn(Optional.of(cachedEntry));

    var response = transactionService().deposit(walletId, amount, correlationId);

    assertThat(response).isEqualTo(previousResponse);
    verify(idempotencyEntryRepository, never()).save(any());
    verify(idempotencyEntryRepository, never()).cache(any());
    verify(walletRepository, never()).findByIdForUpdate(any());
  }

  @Test
  @DisplayName("should save and cache the new idempotency entry after a fresh deposit completes")
  void shouldSaveAndCacheEntryAfterDeposit() {
    var walletId = UUID.randomUUID();
    var correlationId = "corr-3";
    var amount = new BigDecimal("10.00");
    when(idempotencyEntryRepository.findById(correlationId)).thenReturn(Optional.empty());
    when(walletRepository.findByIdForUpdate(walletId))
        .thenReturn(Optional.of(Wallet.of(UUID.randomUUID())));

    transactionService().deposit(walletId, amount, correlationId);

    var savedCaptor = ArgumentCaptor.forClass(IdempotencyEntry.class);
    verify(idempotencyEntryRepository).save(savedCaptor.capture());
    var cachedCaptor = ArgumentCaptor.forClass(IdempotencyEntry.class);
    verify(idempotencyEntryRepository).cache(cachedCaptor.capture());
    assertThat(cachedCaptor.getValue()).isSameAs(savedCaptor.getValue());
    assertThat(cachedCaptor.getValue().getCorrelationId()).isEqualTo(correlationId);
  }
}
