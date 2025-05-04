package com.challenge.wallet.repository;

import com.challenge.wallet.exception.ServiceException;
import com.challenge.wallet.model.Wallet;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static com.challenge.wallet.constants.ValidationMessages.MSG_INSUFFICIENT_BALANCE;

@ApplicationScoped
public class WalletRepositoryImpl implements WalletRepository, PanacheRepositoryBase<Wallet, UUID> {

    @Override
    public Wallet save(Wallet wallet) {
        this.persistAndFlush(wallet);
        return wallet;
    }

    @Override
    public Optional<Wallet> retrieveWallet(UUID walletId) {
        return this.findByIdOptional(walletId);
    }

    @Override
    public void credit(Wallet wallet, BigDecimal amount) {
        this.update("balance = balance + ?1, updatedAt = now() where id = ?2",
                amount, wallet.getId());
    }

    @Override
    public void debit(Wallet wallet, BigDecimal amount) {
        int updatedRows = this.update("balance = balance - ?1, updatedAt = now() where id = ?2 and balance >= ?1",
                amount, wallet.getId());
        if (updatedRows == 0) {
            throw new ServiceException(MSG_INSUFFICIENT_BALANCE.getMessage());
        }
    }
}
