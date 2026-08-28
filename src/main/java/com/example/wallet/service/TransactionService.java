package com.example.wallet.service;

import static com.example.wallet.constants.Messages.SAME_WALLET_TRANSFER;
import static com.example.wallet.entity.TransactionType.DEPOSIT;
import static com.example.wallet.entity.TransactionType.TRANSFER_CREDIT;
import static com.example.wallet.entity.TransactionType.TRANSFER_DEBIT;
import static com.example.wallet.entity.TransactionType.WITHDRAWAL;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

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

  public TransactionService(
      WalletRepository walletRepository, WalletTransactionRepository walletTransactionRepository) {
    this.walletRepository = walletRepository;
    this.walletTransactionRepository = walletTransactionRepository;
  }

  @Transactional
  public void deposit(UUID walletId, BigDecimal amount, UUID idempotencyKey) {
    var wallet = walletRepository.findWallet(walletId);
    wallet.credit(amount);
    walletRepository.save(wallet);

    walletTransactionRepository.save(
        WalletTransaction.builder()
            .walletId(wallet.getId())
            .type(DEPOSIT)
            .amount(amount)
            .balanceAfter(wallet.getBalance())
            .idempotencyKey(idempotencyKey)
            .build());
  }

  @Transactional
  public void withdraw(UUID walletId, BigDecimal amount, UUID idempotencyKey) {
    var wallet = walletRepository.findWallet(walletId);
    wallet.debit(amount);
    walletRepository.save(wallet);

    walletTransactionRepository.save(
        WalletTransaction.builder()
            .walletId(wallet.getId())
            .type(WITHDRAWAL)
            .amount(amount)
            .balanceAfter(wallet.getBalance())
            .idempotencyKey(idempotencyKey)
            .build());
  }

  @Transactional
  public void transfer(UUID fromWalletId, UUID toWalletId, BigDecimal amount, UUID idempotencyKey) {
    validateSelfTransfer(fromWalletId, toWalletId);

    var fromWallet = walletRepository.findWallet(fromWalletId);
    var toWallet = walletRepository.findWallet(toWalletId);
    fromWallet.debit(amount);
    toWallet.credit(amount);
    walletRepository.save(fromWallet);
    walletRepository.save(toWallet);

    walletTransactionRepository.save(
        WalletTransaction.builder()
            .walletId(fromWallet.getId())
            .type(TRANSFER_DEBIT)
            .amount(amount)
            .balanceAfter(fromWallet.getBalance())
            .idempotencyKey(idempotencyKey)
            .peerWalletId(toWallet.getId())
            .build());
    walletTransactionRepository.save(
        WalletTransaction.builder()
            .walletId(toWallet.getId())
            .type(TRANSFER_CREDIT)
            .amount(amount)
            .balanceAfter(toWallet.getBalance())
            .idempotencyKey(idempotencyKey)
            .peerWalletId(fromWallet.getId())
            .build());
  }

  private void validateSelfTransfer(UUID fromWalletId, UUID toWalletId) {
    if (fromWalletId.equals(toWalletId)) {
      throw ServiceException.of(SAME_WALLET_TRANSFER, BAD_REQUEST);
    }
  }
}
