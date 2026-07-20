package com.example.wallet_service.service;

import com.example.wallet_service.dto.BalanceResponse;
import com.example.wallet_service.dto.WalletResponse;
import com.example.wallet_service.entity.Wallet;
import com.example.wallet_service.entity.WalletTransaction;
import com.example.wallet_service.mapper.WalletMapper;
import com.example.wallet_service.repository.WalletRepository;
import com.example.wallet_service.repository.WalletTransactionRepository;
import com.example.wallet_service.service.exception.InvalidAsOfException;
import com.example.wallet_service.service.exception.WalletAlreadyExistsException;
import com.example.wallet_service.service.exception.WalletNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletMapper walletMapper;

    public WalletService(WalletRepository walletRepository, WalletTransactionRepository walletTransactionRepository,
            WalletMapper walletMapper) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.walletMapper = walletMapper;
    }

    @Transactional
    public WalletResponse createWallet(String userId) {
        if (walletRepository.existsByUserId(userId)) {
            throw new WalletAlreadyExistsException(userId);
        }
        Wallet wallet = Wallet.createNew(userId);
        try {
            walletRepository.saveAndFlush(wallet);
        } catch (DataIntegrityViolationException ex) {
            throw new WalletAlreadyExistsException(userId);
        }
        return walletMapper.toWalletResponse(wallet);
    }

    @Transactional(readOnly = true)
    public Wallet findByUserIdOrThrow(String userId) {
        return walletRepository.findByUserId(userId).orElseThrow(() -> new WalletNotFoundException(userId));
    }

    @Transactional(readOnly = true)
    public BalanceResponse getCurrentBalance(String userId) {
        Wallet wallet = findByUserIdOrThrow(userId);
        return new BalanceResponse(userId, wallet.getBalance(), Instant.now());
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalanceAsOf(String userId, Instant asOf) {
        if (asOf.isAfter(Instant.now())) {
            throw new InvalidAsOfException("asOf must not be in the future: " + asOf);
        }
        Wallet wallet = findByUserIdOrThrow(userId);
        BigDecimal balance = walletTransactionRepository
                .findTopByWalletIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(wallet.getId(), asOf)
                .map(WalletTransaction::getBalanceAfter)
                .orElse(BigDecimal.ZERO.setScale(2));
        return new BalanceResponse(userId, balance, asOf);
    }
}
