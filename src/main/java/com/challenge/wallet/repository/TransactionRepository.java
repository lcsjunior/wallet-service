package com.challenge.wallet.repository;

import com.challenge.wallet.model.Transaction;

public interface TransactionRepository {

    Transaction save(Transaction transaction);
}
