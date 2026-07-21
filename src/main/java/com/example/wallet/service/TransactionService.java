package com.example.wallet.service;

import static com.example.wallet.exception.ErrorCode.CORRELATION_ID_CONFLICT;
import static com.example.wallet.exception.ErrorCode.INSUFFICIENT_BALANCE;
import static com.example.wallet.exception.ErrorCode.SAME_WALLET_TRANSFER;
import static com.example.wallet.exception.ErrorCode.WALLET_NOT_FOUND;

import com.example.wallet.dto.TransactionResponse;
import com.example.wallet.dto.TransferResponse;
import com.example.wallet.entity.IdempotencyEntry;
import com.example.wallet.entity.OperationType;
import com.example.wallet.entity.TransactionType;
import com.example.wallet.entity.Wallet;
import com.example.wallet.entity.WalletTransaction;
import com.example.wallet.exception.ServiceException;
import com.example.wallet.repository.IdempotencyEntryRepository;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.repository.WalletTransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  private final ObjectMapper objectMapper;

  public TransactionService(
      WalletRepository walletRepository,
      WalletTransactionRepository walletTransactionRepository,
      IdempotencyEntryRepository idempotencyEntryRepository,
      ObjectMapper objectMapper) {
    this.walletRepository = walletRepository;
    this.walletTransactionRepository = walletTransactionRepository;
    this.idempotencyEntryRepository = idempotencyEntryRepository;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public void deposit(UUID walletId, BigDecimal amount, String correlationId) {
    var fingerprint = buildFingerprint(OperationType.DEPOSIT, walletId.toString(), amount);

    if (isReplay(correlationId, fingerprint)) {
      return;
    }

    var wallet = findWalletForUpdate(walletId);
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

    var response =
        TransactionResponse.of(wallet.getId(), wallet.getBalance(), amount, correlationId);
    persistIdempotencyEntry(correlationId, OperationType.DEPOSIT, fingerprint, response);
    log.info(
        LOG_PREFIX + "Deposit completed | walletId={}, amount={}, correlationId={}",
        walletId,
        amount,
        correlationId);
  }

  @Transactional
  public void withdraw(UUID walletId, BigDecimal amount, String correlationId) {
    var fingerprint = buildFingerprint(OperationType.WITHDRAWAL, walletId.toString(), amount);

    if (isReplay(correlationId, fingerprint)) {
      return;
    }

    var wallet = findWalletForUpdate(walletId);
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

    var response =
        TransactionResponse.of(wallet.getId(), wallet.getBalance(), amount, correlationId);
    persistIdempotencyEntry(correlationId, OperationType.WITHDRAWAL, fingerprint, response);
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

    if (isReplay(correlationId, fingerprint)) {
      return;
    }

    var firstWalletId = fromWalletId.compareTo(toWalletId) <= 0 ? fromWalletId : toWalletId;
    var secondWalletId = fromWalletId.compareTo(toWalletId) <= 0 ? toWalletId : fromWalletId;
    var firstWallet = findWalletForUpdate(firstWalletId);
    var secondWallet = findWalletForUpdate(secondWalletId);

    var fromWallet = firstWalletId.equals(fromWalletId) ? firstWallet : secondWallet;
    var toWallet = firstWalletId.equals(fromWalletId) ? secondWallet : firstWallet;

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

    var response =
        TransferResponse.of(
            fromWallet.getId(),
            fromWallet.getBalance(),
            toWallet.getId(),
            toWallet.getBalance(),
            amount,
            correlationId);
    persistIdempotencyEntry(correlationId, OperationType.TRANSFER, fingerprint, response);
    log.info(
        LOG_PREFIX
            + "Transfer completed | fromWalletId={}, toWalletId={}, amount={}, correlationId={}",
        fromWalletId,
        toWalletId,
        amount,
        correlationId);
  }

  private Wallet findWalletForUpdate(UUID walletId) {
    return walletRepository
        .findByIdForUpdate(walletId)
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

  private void validateDistinctWallets(UUID fromWalletId, UUID toWalletId) {
    if (fromWalletId.equals(toWalletId)) {
      throw new ServiceException(SAME_WALLET_TRANSFER);
    }
  }

  private String buildFingerprint(OperationType operationType, String key, BigDecimal amount) {
    return operationType + ":" + key + ":" + amount.toPlainString();
  }

  private boolean isReplay(String correlationId, String fingerprint) {
    var existing = idempotencyEntryRepository.findById(correlationId);
    if (existing.isEmpty()) {
      return false;
    }
    validateFingerprint(correlationId, fingerprint, existing.get().getRequestFingerprint());
    return true;
  }

  private void validateFingerprint(
      String correlationId, String expectedFingerprint, String actualFingerprint) {
    if (!actualFingerprint.equals(expectedFingerprint)) {
      log.warn(LOG_PREFIX + "Correlation-Id conflict | correlationId={}", correlationId);
      throw new ServiceException(CORRELATION_ID_CONFLICT);
    }
  }

  private void persistIdempotencyEntry(
      String correlationId, OperationType operationType, String fingerprint, Object response) {
    try {
      var body = objectMapper.writeValueAsString(response);
      var entry = IdempotencyEntry.of(correlationId, operationType, fingerprint, body);
      idempotencyEntryRepository.save(entry);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException(
          "Failed to serialize idempotency entry for " + correlationId, ex);
    }
  }
}
