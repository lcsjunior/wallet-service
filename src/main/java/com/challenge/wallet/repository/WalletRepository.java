package com.challenge.wallet.repository;

import com.challenge.wallet.model.Wallet;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository {

    Wallet save(Wallet wallet);

    Optional<Wallet> retrieveWallet(UUID walletId);

    void credit(Wallet wallet, BigDecimal amount);

    void debit(Wallet wallet, BigDecimal amount);
}
