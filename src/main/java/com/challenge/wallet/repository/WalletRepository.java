package com.challenge.wallet.repository;

import com.challenge.wallet.model.Wallet;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository {

    Wallet save(Wallet wallet);

    Optional<Wallet> getWallet(UUID walletId);
}
