package com.challenge.wallet.repository;

import com.challenge.wallet.model.Wallet;

import java.util.UUID;

public interface WalletRepository {

    Wallet persistWallet(Wallet wallet);

    Wallet getWallet(UUID walletId);
}
