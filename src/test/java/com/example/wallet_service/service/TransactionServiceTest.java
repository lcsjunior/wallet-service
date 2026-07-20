package com.example.wallet_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.wallet_service.dto.TransactionResponse;
import com.example.wallet_service.dto.TransferResponse;
import com.example.wallet_service.entity.IdempotencyRecord;
import com.example.wallet_service.entity.OperationType;
import com.example.wallet_service.entity.Wallet;
import com.example.wallet_service.repository.IdempotencyRecordRepository;
import com.example.wallet_service.repository.WalletRepository;
import com.example.wallet_service.repository.WalletTransactionRepository;
import com.example.wallet_service.service.exception.IdempotencyConflictException;
import com.example.wallet_service.service.exception.InsufficientBalanceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(walletRepository, walletTransactionRepository,
                idempotencyRecordRepository, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    @DisplayName("Não deve duplicar o depósito quando o mesmo correlationId é reprocessado")
    void shouldNotDuplicateDepositWhenCorrelationIdIsRetried() throws Exception {
        Wallet wallet = Wallet.createNew("alice");
        when(walletRepository.findByUserIdForUpdate("alice")).thenReturn(Optional.of(wallet));
        when(idempotencyRecordRepository.findById("dep-1")).thenReturn(Optional.empty());

        ArgumentCaptor<IdempotencyRecord> captor = ArgumentCaptor.forClass(IdempotencyRecord.class);

        transactionService.deposit("alice", new BigDecimal("100.00"), "dep-1");

        verify(walletTransactionRepository, times(1)).save(any());
        verify(idempotencyRecordRepository, times(1)).save(captor.capture());

        IdempotencyRecord savedRecord = captor.getValue();
        when(idempotencyRecordRepository.findById("dep-1")).thenReturn(Optional.of(savedRecord));

        TransactionResponse secondResult = transactionService.deposit("alice", new BigDecimal("100.00"), "dep-1");

        assertThat(secondResult.balance()).isEqualByComparingTo("100.00");
        verify(walletTransactionRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar retry com o mesmo correlationId e valor diferente")
    void shouldRejectRetryWithSameCorrelationIdAndDifferentAmount() {
        IdempotencyRecord existing = IdempotencyRecord.create("dep-1", OperationType.DEPOSIT,
                "DEPOSIT:alice:100.00", "{}");
        when(idempotencyRecordRepository.findById("dep-1")).thenReturn(Optional.of(existing));

        org.junit.jupiter.api.Assertions.assertThrows(IdempotencyConflictException.class,
                () -> transactionService.deposit("alice", new BigDecimal("999.00"), "dep-1"));

        verify(walletRepository, never()).findByUserIdForUpdate(any());
        verify(walletTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve persistir movimentação quando o saque excede o saldo disponível")
    void shouldNotPersistTransactionWhenWithdrawalExceedsBalance() throws Exception {
        Wallet wallet = Wallet.createNew("alice");
        wallet.credit(new BigDecimal("50.00"));
        when(walletRepository.findByUserIdForUpdate("alice")).thenReturn(Optional.of(wallet));
        when(idempotencyRecordRepository.findById("wd-1")).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(InsufficientBalanceException.class,
                () -> transactionService.withdraw("alice", new BigDecimal("1000.00"), "wd-1"));

        verify(walletTransactionRepository, never()).save(any());
        verify(idempotencyRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve perder ou criar dinheiro em uma transferência (soma dos saldos preservada)")
    void shouldPreserveTotalBalanceAcrossTransfer() {
        Wallet alice = Wallet.createNew("alice");
        alice.credit(new BigDecimal("100.00"));
        Wallet bob = Wallet.createNew("bob");
        bob.credit(new BigDecimal("20.00"));

        when(walletRepository.findByUserIdForUpdate("alice")).thenReturn(Optional.of(alice));
        when(walletRepository.findByUserIdForUpdate("bob")).thenReturn(Optional.of(bob));
        when(idempotencyRecordRepository.findById("tx-1")).thenReturn(Optional.empty());

        BigDecimal totalBefore = alice.getBalance().add(bob.getBalance());

        TransferResponse response = transactionService.transfer("alice", "bob", new BigDecimal("30.00"), "tx-1");

        BigDecimal totalAfter = response.fromBalance().add(response.toBalance());
        assertThat(totalAfter).isEqualByComparingTo(totalBefore);
        assertThat(response.fromBalance()).isEqualByComparingTo("70.00");
        assertThat(response.toBalance()).isEqualByComparingTo("50.00");
    }
}
