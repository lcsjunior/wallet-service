package com.example.wallet.service;

import static com.example.wallet.constants.Messages.CORRELATION_ID_CONFLICT;
import static com.example.wallet.constants.Messages.INSUFFICIENT_BALANCE;
import static com.example.wallet.constants.Messages.SAME_WALLET_TRANSFER;
import static com.example.wallet.dto.TransactionOutcome.APPLIED;
import static com.example.wallet.dto.TransactionOutcome.REPLAYED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import com.example.wallet.dto.TransactionOutcome;
import com.example.wallet.entity.IdempotencyEntry;
import com.example.wallet.entity.OperationType;
import com.example.wallet.entity.TransactionType;
import com.example.wallet.entity.Wallet;
import com.example.wallet.entity.WalletTransaction;
import com.example.wallet.exception.ServiceException;
import com.example.wallet.repository.IdempotencyRepository;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.repository.WalletTransactionRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

  private static final String LOG_PREFIX = "[TRANSACTION_SERVICE] ";

  private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

  private final WalletRepository walletRepository;
  private final WalletTransactionRepository walletTransactionRepository;
  private final IdempotencyRepository idempotencyRepository;

  public TransactionService(
      WalletRepository walletRepository,
      WalletTransactionRepository walletTransactionRepository,
      IdempotencyRepository idempotencyRepository) {
    this.walletRepository = walletRepository;
    this.walletTransactionRepository = walletTransactionRepository;
    this.idempotencyRepository = idempotencyRepository;
  }

  @Transactional
  public TransactionOutcome deposit(UUID walletId, BigDecimal amount, UUID correlationId) {
    var idempotencyEntry =
        IdempotencyEntry.of(correlationId, OperationType.DEPOSIT, walletId.toString(), amount);
    if (isReplay(idempotencyEntry)) {
      return REPLAYED;
    }

    var wallet = walletRepository.findWallet(walletId);
    wallet.credit(amount);
    walletRepository.save(wallet);

    var transaction =
        WalletTransaction.builder()
            .walletId(wallet.getId())
            .type(TransactionType.DEPOSIT)
            .amount(amount)
            .balanceAfter(wallet.getBalance())
            .correlationId(correlationId)
            .build();
    walletTransactionRepository.save(transaction);

    idempotencyRepository.save(idempotencyEntry);

    return APPLIED;
  }

  @Transactional
  public TransactionOutcome withdraw(UUID walletId, BigDecimal amount, UUID correlationId) {
    var idempotencyEntry =
        IdempotencyEntry.of(correlationId, OperationType.WITHDRAWAL, walletId.toString(), amount);
    if (isReplay(idempotencyEntry)) {
      return REPLAYED;
    }

    var wallet = walletRepository.findWallet(walletId);
    validateSufficientBalance(wallet, amount);
    wallet.debit(amount);
    walletRepository.save(wallet);

    var transaction =
        WalletTransaction.builder()
            .walletId(wallet.getId())
            .type(TransactionType.WITHDRAWAL)
            .amount(amount)
            .balanceAfter(wallet.getBalance())
            .correlationId(correlationId)
            .build();
    walletTransactionRepository.save(transaction);

    idempotencyRepository.save(idempotencyEntry);

    return APPLIED;
  }

  @Transactional
  public TransactionOutcome transfer(
      UUID fromWalletId, UUID toWalletId, BigDecimal amount, UUID correlationId) {
    rejectSelfTransfer(fromWalletId, toWalletId);
    var idempotencyEntry =
        IdempotencyEntry.of(
            correlationId, OperationType.TRANSFER, fromWalletId + "->" + toWalletId, amount);
    if (isReplay(idempotencyEntry)) {
      return REPLAYED;
    }

    var fromWallet = walletRepository.findWallet(fromWalletId);
    validateSufficientBalance(fromWallet, amount);
    var toWallet = walletRepository.findWallet(toWalletId);
    fromWallet.debit(amount);
    toWallet.credit(amount);
    walletRepository.save(fromWallet);
    walletRepository.save(toWallet);

    walletTransactionRepository.save(
        WalletTransaction.builder()
            .walletId(fromWallet.getId())
            .type(TransactionType.TRANSFER_DEBIT)
            .amount(amount)
            .balanceAfter(fromWallet.getBalance())
            .correlationId(correlationId)
            .peerWalletId(toWallet.getId())
            .build());
    walletTransactionRepository.save(
        WalletTransaction.builder()
            .walletId(toWallet.getId())
            .type(TransactionType.TRANSFER_CREDIT)
            .amount(amount)
            .balanceAfter(toWallet.getBalance())
            .correlationId(correlationId)
            .peerWalletId(fromWallet.getId())
            .build());

    idempotencyRepository.save(idempotencyEntry);

    return APPLIED;
  }

  private void rejectSelfTransfer(UUID fromWalletId, UUID toWalletId) {
    if (fromWalletId.equals(toWalletId)) {
      throw ServiceException.of(SAME_WALLET_TRANSFER, BAD_REQUEST);
    }
  }

  private boolean isReplay(IdempotencyEntry idempotencyEntry) {
    var correlationId = idempotencyEntry.getCorrelationId();
    var storedEntry = idempotencyRepository.findById(correlationId);
    if (storedEntry.isEmpty()) {
      return false;
    }
    var storedFingerprint = storedEntry.get().getRequestFingerprint();
    if (!storedFingerprint.equals(idempotencyEntry.getRequestFingerprint())) {
      throw ServiceException.of(CORRELATION_ID_CONFLICT, CONFLICT);
    }
    log.info(LOG_PREFIX + "Duplicate request ignored | correlationId={}", correlationId);
    return true;
  }

  private void validateSufficientBalance(Wallet wallet, BigDecimal amount) {
    if (wallet.getBalance().compareTo(amount) < 0) {
      throw ServiceException.of(INSUFFICIENT_BALANCE, UNPROCESSABLE_ENTITY);
    }
  }
}
