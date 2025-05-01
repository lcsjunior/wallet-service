package com.challenge.wallet.repository;

import com.challenge.wallet.exception.WalletNotFoundException;
import com.challenge.wallet.model.Wallet;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class WalletRepositoryImpl implements WalletRepository, PanacheRepositoryBase<Wallet, UUID> {

    @Override
    public Wallet save(Wallet wallet) {
        this.persistAndFlush(wallet);
        return wallet;
    }

    @Override
    public Wallet getWallet(UUID walletId) {
        return this.findByIdOptional(walletId).orElseThrow(WalletNotFoundException::new);
    }
}
