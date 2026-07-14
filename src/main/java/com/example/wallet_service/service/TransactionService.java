package com.example.wallet_service.service;

import com.example.wallet_service.dto.TransactionResponse;
import com.example.wallet_service.dto.TransferResponse;
import com.example.wallet_service.entity.IdempotencyRecord;
import com.example.wallet_service.entity.OperationType;
import com.example.wallet_service.entity.TransactionType;
import com.example.wallet_service.entity.Wallet;
import com.example.wallet_service.entity.WalletTransaction;
import com.example.wallet_service.repository.IdempotencyRecordRepository;
import com.example.wallet_service.repository.WalletRepository;
import com.example.wallet_service.repository.WalletTransactionRepository;
import com.example.wallet_service.service.exception.IdempotencyConflictException;
import com.example.wallet_service.service.exception.InsufficientBalanceException;
import com.example.wallet_service.service.exception.SameWalletTransferException;
import com.example.wallet_service.service.exception.WalletNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    public TransactionService(WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository,
            IdempotencyRecordRepository idempotencyRecordRepository,
            ObjectMapper objectMapper) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TransactionResponse deposit(String userId, BigDecimal amount, String correlationId) {
        BigDecimal normalizedAmount = normalize(amount);
        String fingerprint = fingerprint(OperationType.DEPOSIT, userId, normalizedAmount);

        Optional<TransactionResponse> cached = replayIfIdempotent(correlationId, fingerprint);
        if (cached.isPresent()) {
            return cached.get();
        }

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId).orElseThrow(() -> new WalletNotFoundException(userId));
        wallet.credit(normalizedAmount);
        walletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.create(wallet.getId(), TransactionType.DEPOSIT,
                normalizedAmount, wallet.getBalance(), correlationId, null);
        walletTransactionRepository.save(transaction);

        TransactionResponse response = new TransactionResponse(userId, wallet.getBalance(), normalizedAmount,
                correlationId);
        persistIdempotencyRecord(correlationId, OperationType.DEPOSIT, fingerprint, response);
        return response;
    }

    @Transactional
    public TransactionResponse withdraw(String userId, BigDecimal amount, String correlationId) {
        BigDecimal normalizedAmount = normalize(amount);
        String fingerprint = fingerprint(OperationType.WITHDRAWAL, userId, normalizedAmount);

        Optional<TransactionResponse> cached = replayIfIdempotent(correlationId, fingerprint);
        if (cached.isPresent()) {
            return cached.get();
        }

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId).orElseThrow(() -> new WalletNotFoundException(userId));
        ensureSufficientBalance(wallet, normalizedAmount, userId);
        wallet.debit(normalizedAmount);
        walletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.create(wallet.getId(), TransactionType.WITHDRAWAL,
                normalizedAmount, wallet.getBalance(), correlationId, null);
        walletTransactionRepository.save(transaction);

        TransactionResponse response = new TransactionResponse(userId, wallet.getBalance(), normalizedAmount,
                correlationId);
        persistIdempotencyRecord(correlationId, OperationType.WITHDRAWAL, fingerprint, response);
        return response;
    }

    @Transactional
    public TransferResponse transfer(String fromUserId, String toUserId, BigDecimal amount, String correlationId) {
        if (fromUserId.equals(toUserId)) {
            throw new SameWalletTransferException(fromUserId);
        }
        BigDecimal normalizedAmount = normalize(amount);
        String fingerprint = fingerprint(OperationType.TRANSFER, fromUserId + "->" + toUserId, normalizedAmount);

        Optional<TransferResponse> cached = idempotencyRecordRepository.findById(correlationId).map(record -> {
            if (!record.getRequestFingerprint().equals(fingerprint)) {
                throw new IdempotencyConflictException(correlationId);
            }
            return readTransferResponse(record);
        });
        if (cached.isPresent()) {
            return cached.get();
        }

        String firstUserId = fromUserId.compareTo(toUserId) <= 0 ? fromUserId : toUserId;
        String secondUserId = fromUserId.compareTo(toUserId) <= 0 ? toUserId : fromUserId;
        Wallet firstWallet = walletRepository.findByUserIdForUpdate(firstUserId).orElseThrow(() -> new WalletNotFoundException(firstUserId));
        Wallet secondWallet = walletRepository.findByUserIdForUpdate(secondUserId).orElseThrow(() -> new WalletNotFoundException(secondUserId));

        Wallet fromWallet = firstUserId.equals(fromUserId) ? firstWallet : secondWallet;
        Wallet toWallet = firstUserId.equals(fromUserId) ? secondWallet : firstWallet;

        ensureSufficientBalance(fromWallet, normalizedAmount, fromUserId);
        fromWallet.debit(normalizedAmount);
        toWallet.credit(normalizedAmount);
        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);

        walletTransactionRepository.save(WalletTransaction.create(fromWallet.getId(), TransactionType.TRANSFER_DEBIT,
                normalizedAmount, fromWallet.getBalance(), correlationId, toWallet.getId()));
        walletTransactionRepository.save(WalletTransaction.create(toWallet.getId(), TransactionType.TRANSFER_CREDIT,
                normalizedAmount, toWallet.getBalance(), correlationId, fromWallet.getId()));

        TransferResponse response = new TransferResponse(fromUserId, fromWallet.getBalance(), toUserId,
                toWallet.getBalance(), normalizedAmount, correlationId);
        persistIdempotencyRecord(correlationId, OperationType.TRANSFER, fingerprint, response);
        return response;
    }

    private TransferResponse readTransferResponse(IdempotencyRecord record) {
        try {
            return objectMapper.readValue(record.getResultBody(), TransferResponse.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Corrupted idempotency record for " + record.getCorrelationId(), ex);
        }
    }

    private BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    private String fingerprint(OperationType operationType, String key, BigDecimal amount) {
        return operationType + ":" + key + ":" + amount.toPlainString();
    }

    private Optional<TransactionResponse> replayIfIdempotent(String correlationId, String fingerprint) {
        return idempotencyRecordRepository.findById(correlationId).map(record -> {
            if (!record.getRequestFingerprint().equals(fingerprint)) {
                throw new IdempotencyConflictException(correlationId);
            }
            return readResponse(record);
        });
    }

    private TransactionResponse readResponse(IdempotencyRecord record) {
        try {
            return objectMapper.readValue(record.getResultBody(), TransactionResponse.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Corrupted idempotency record for " + record.getCorrelationId(), ex);
        }
    }

    private void persistIdempotencyRecord(String correlationId, OperationType operationType, String fingerprint,
            Object response) {
        try {
            String body = objectMapper.writeValueAsString(response);
            idempotencyRecordRepository.save(IdempotencyRecord.create(correlationId, operationType, fingerprint, body));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize idempotency record for " + correlationId, ex);
        }
    }

    private void ensureSufficientBalance(Wallet wallet, BigDecimal amount, String userId) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(userId);
        }
    }
}
