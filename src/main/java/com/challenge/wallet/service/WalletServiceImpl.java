package com.challenge.wallet.service;

import com.challenge.wallet.dto.BalanceResponse;
import com.challenge.wallet.dto.CreateWalletResponse;
import com.challenge.wallet.model.Wallet;
import com.challenge.wallet.repository.WalletRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class WalletServiceImpl implements WalletService {

    @Inject
    WalletRepository walletRepository;

    @Override
    @Transactional
    public CreateWalletResponse createWallet() {
        Wallet wallet = walletRepository.persistWallet(new Wallet());
        return CreateWalletResponse.from(wallet);
    }

    @Override
    public BalanceResponse getWallet(UUID walletId) {
        Wallet wallet = walletRepository.getWallet(walletId);
        return BalanceResponse.from(wallet);
    }
}
