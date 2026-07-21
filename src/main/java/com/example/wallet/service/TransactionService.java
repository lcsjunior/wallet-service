package com.example.wallet.service;

import static com.example.wallet.exception.ErrorCode.CORRELATION_ID_CONFLICT;
import static com.example.wallet.exception.ErrorCode.INSUFFICIENT_BALANCE;
import static com.example.wallet.exception.ErrorCode.SAME_WALLET_TRANSFER;
import static com.example.wallet.exception.ErrorCode.WALLET_NOT_FOUND;

import com.example.wallet.entity.IdempotencyEntry;
import com.example.wallet.entity.OperationType;
import com.example.wallet.entity.TransactionType;
import com.example.wallet.entity.Wallet;
import com.example.wallet.entity.WalletTransaction;
import com.example.wallet.exception.ServiceException;
import com.example.wallet.repository.IdempotencyEntryRepository;
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
  private final IdempotencyEntryRepository idempotencyEntryRepository;

  public TransactionService(
      WalletRepository walletRepository,
      WalletTransactionRepository walletTransactionRepository,
      IdempotencyEntryRepository idempotencyEntryRepository) {
    this.walletRepository = walletRepository;
    this.walletTransactionRepository = walletTransactionRepository;
    this.idempotencyEntryRepository = idempotencyEntryRepository;
  }

  @Transactional
  public void deposit(UUID walletId, BigDecimal amount, String correlationId) {
    var fingerprint = buildFingerprint(OperationType.DEPOSIT, walletId.toString(), amount);

    if (isDuplicateRequest(correlationId, fingerprint)) {
      return;
    }

    var wallet = findWallet(walletId);
    wallet.credit(amount);
    walletRepository.save(wallet);

    var transaction =
        WalletTransaction.of(
            wallet.getId(),
            TransactionType.DEPOSIT,
            amount,
            wallet.getBalance(),
            correlationId,
            null);
    walletTransactionRepository.save(transaction);

    persistIdempotency(correlationId, OperationType.DEPOSIT, fingerprint);
    log.info(
        LOG_PREFIX + "Deposit completed | walletId={}, amount={}, correlationId={}",
        walletId,
        amount,
        correlationId);
  }

  @Transactional
  public void withdraw(UUID walletId, BigDecimal amount, String correlationId) {
    var fingerprint = buildFingerprint(OperationType.WITHDRAWAL, walletId.toString(), amount);

    if (isDuplicateRequest(correlationId, fingerprint)) {
      return;
    }

    var wallet = findWallet(walletId);
    validateSufficientBalance(wallet, amount);
    wallet.debit(amount);
    walletRepository.save(wallet);

    var transaction =
        WalletTransaction.of(
            wallet.getId(),
            TransactionType.WITHDRAWAL,
            amount,
            wallet.getBalance(),
            correlationId,
            null);
    walletTransactionRepository.save(transaction);

    persistIdempotency(correlationId, OperationType.WITHDRAWAL, fingerprint);
    log.info(
        LOG_PREFIX + "Withdrawal completed | walletId={}, amount={}, correlationId={}",
        walletId,
        amount,
        correlationId);
  }

  @Transactional
  public void transfer(
      UUID fromWalletId, UUID toWalletId, BigDecimal amount, String correlationId) {
    validateDistinctWallets(fromWalletId, toWalletId);
    var fingerprint =
        buildFingerprint(OperationType.TRANSFER, fromWalletId + "->" + toWalletId, amount);

    if (isDuplicateRequest(correlationId, fingerprint)) {
      return;
    }

    var fromWallet = findWallet(fromWalletId);
    var toWallet = findWallet(toWalletId);

    validateSufficientBalance(fromWallet, amount);
    fromWallet.debit(amount);
    toWallet.credit(amount);
    walletRepository.save(fromWallet);
    walletRepository.save(toWallet);

    walletTransactionRepository.save(
        WalletTransaction.of(
            fromWallet.getId(),
            TransactionType.TRANSFER_DEBIT,
            amount,
            fromWallet.getBalance(),
            correlationId,
            toWallet.getId()));
    walletTransactionRepository.save(
        WalletTransaction.of(
            toWallet.getId(),
            TransactionType.TRANSFER_CREDIT,
            amount,
            toWallet.getBalance(),
            correlationId,
            fromWallet.getId()));

    persistIdempotency(correlationId, OperationType.TRANSFER, fingerprint);
    log.info(
        LOG_PREFIX
            + "Transfer completed | fromWalletId={}, toWalletId={}, amount={}, correlationId={}",
        fromWalletId,
        toWalletId,
        amount,
        correlationId);
  }

  private void validateDistinctWallets(UUID fromWalletId, UUID toWalletId) {
    if (fromWalletId.equals(toWalletId)) {
      throw new ServiceException(SAME_WALLET_TRANSFER);
    }
  }

  private String buildFingerprint(OperationType operationType, String key, BigDecimal amount) {
    return operationType + ":" + key + ":" + amount.toPlainString();
  }

  private boolean isDuplicateRequest(String correlationId, String fingerprint) {
    var entryOpt = idempotencyEntryRepository.findById(correlationId);
    if (entryOpt.isEmpty()) {
      return false;
    }
    validateFingerprint(correlationId, fingerprint, entryOpt.get().getRequestFingerprint());
    log.info(LOG_PREFIX + "Duplicate request ignored | correlationId={}", correlationId);
    return true;
  }

  private void validateFingerprint(
      String correlationId, String expectedFingerprint, String actualFingerprint) {
    if (!actualFingerprint.equals(expectedFingerprint)) {
      log.warn(LOG_PREFIX + "Correlation-Id conflict | correlationId={}", correlationId);
      throw new ServiceException(CORRELATION_ID_CONFLICT);
    }
  }

  private Wallet findWallet(UUID walletId) {
    return walletRepository
        .findById(walletId)
        .orElseThrow(() -> walletNotFoundException(walletId));
  }

  private ServiceException walletNotFoundException(UUID walletId) {
    log.warn(LOG_PREFIX + "Wallet not found | walletId={}", walletId);
    return new ServiceException(WALLET_NOT_FOUND);
  }

  private void validateSufficientBalance(Wallet wallet, BigDecimal amount) {
    if (wallet.getBalance().compareTo(amount) < 0) {
      log.warn(
          LOG_PREFIX + "Insufficient balance | walletId={}, amount={}", wallet.getId(), amount);
      throw new ServiceException(INSUFFICIENT_BALANCE);
    }
  }

  private void persistIdempotency(
      String correlationId, OperationType operationType, String fingerprint) {
    var entry = IdempotencyEntry.of(correlationId, operationType, fingerprint);
    idempotencyEntryRepository.save(entry);
  }
}
