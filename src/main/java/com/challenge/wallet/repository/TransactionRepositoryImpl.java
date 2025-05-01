package com.challenge.wallet.repository;

import com.challenge.wallet.model.Transaction;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class TransactionRepositoryImpl implements TransactionRepository, PanacheRepositoryBase<Transaction, UUID> {

    @Override
    public Transaction save(Transaction transaction) {
        this.persistAndFlush(transaction);
        return transaction;
    }
}
