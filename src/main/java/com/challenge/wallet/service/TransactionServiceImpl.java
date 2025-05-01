package com.challenge.wallet.service;

import com.challenge.wallet.model.Transaction;
import com.challenge.wallet.model.TransactionType;
import com.challenge.wallet.model.Wallet;
import com.challenge.wallet.repository.TransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;

@ApplicationScoped
public class TransactionServiceImpl implements TransactionService {

    @Inject
    TransactionRepository transactionRepository;

    @Transactional
    @Override
    public Transaction createTransaction(Wallet wallet, BigDecimal amount, TransactionType type,
                                         Transaction relatedTransaction) {
        return transactionRepository.save(new Transaction(wallet, amount, type, relatedTransaction));
    }

    @Transactional
    @Override
    public Transaction createTransaction(Wallet wallet, BigDecimal amount, TransactionType type) {
        return transactionRepository.save(new Transaction(wallet, amount, type, null));
    }
}
