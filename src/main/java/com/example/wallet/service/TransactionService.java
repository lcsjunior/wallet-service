package com.example.wallet.service;

import static com.example.wallet.constants.Messages.SAME_WALLET_TRANSFER;
import static com.example.wallet.dto.TransactionOutcome.APPLIED;
import static com.example.wallet.dto.TransactionOutcome.REPLAYED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import com.example.wallet.dto.TransactionOutcome;
import com.example.wallet.entity.IdempotencyEntry;
import com.example.wallet.entity.OperationType;
import com.example.wallet.entity.TransactionType;
import com.example.wallet.entity.WalletTransaction;
import com.example.wallet.exception.ServiceException;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.repository.WalletTransactionRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

  private final WalletRepository walletRepository;
  private final WalletTransactionRepository walletTransactionRepository;
  private final IdempotencyService idempotencyService;

  public TransactionService(
      WalletRepository walletRepository,
      WalletTransactionRepository walletTransactionRepository,
      IdempotencyService idempotencyService) {
    this.walletRepository = walletRepository;
    this.walletTransactionRepository = walletTransactionRepository;
    this.idempotencyService = idempotencyService;
  }

  @Transactional
  public TransactionOutcome deposit(UUID walletId, BigDecimal amount, UUID correlationId) {
    var idempotencyEntry =
        IdempotencyEntry.of(correlationId, OperationType.DEPOSIT, walletId.toString(), amount);
    if (idempotencyService.isReplay(idempotencyEntry)) {
      return REPLAYED;
    }

    var wallet = walletRepository.findWallet(walletId);
    wallet.credit(amount);
    walletRepository.save(wallet);

    walletTransactionRepository.save(
        WalletTransaction.builder()
            .walletId(wallet.getId())
            .type(TransactionType.DEPOSIT)
            .amount(amount)
            .balanceAfter(wallet.getBalance())
            .correlationId(correlationId)
            .build());

    idempotencyService.save(idempotencyEntry);

    return APPLIED;
  }

  @Transactional
  public TransactionOutcome withdraw(UUID walletId, BigDecimal amount, UUID correlationId) {
    var idempotencyEntry =
        IdempotencyEntry.of(correlationId, OperationType.WITHDRAWAL, walletId.toString(), amount);
    if (idempotencyService.isReplay(idempotencyEntry)) {
      return REPLAYED;
    }

    var wallet = walletRepository.findWallet(walletId);
    wallet.debit(amount);
    walletRepository.save(wallet);

    walletTransactionRepository.save(
        WalletTransaction.builder()
            .walletId(wallet.getId())
            .type(TransactionType.WITHDRAWAL)
            .amount(amount)
            .balanceAfter(wallet.getBalance())
            .correlationId(correlationId)
            .build());

    idempotencyService.save(idempotencyEntry);

    return APPLIED;
  }

  @Transactional
  public TransactionOutcome transfer(
      UUID fromWalletId, UUID toWalletId, BigDecimal amount, UUID correlationId) {
    validateSelfTransfer(fromWalletId, toWalletId);
    var idempotencyEntry =
        IdempotencyEntry.of(
            correlationId, OperationType.TRANSFER, fromWalletId + "->" + toWalletId, amount);
    if (idempotencyService.isReplay(idempotencyEntry)) {
      return REPLAYED;
    }

    var fromWallet = walletRepository.findWallet(fromWalletId);
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

    idempotencyService.save(idempotencyEntry);

    return APPLIED;
  }

  private void validateSelfTransfer(UUID fromWalletId, UUID toWalletId) {
    if (fromWalletId.equals(toWalletId)) {
      throw ServiceException.of(SAME_WALLET_TRANSFER, BAD_REQUEST);
    }
  }
}
