package com.challenge.wallet.service;

import com.challenge.wallet.model.Transaction;
import com.challenge.wallet.model.TransactionType;
import com.challenge.wallet.model.Wallet;

import java.math.BigDecimal;

public interface TransactionService {

    Transaction createTransaction(Wallet wallet, BigDecimal amount, TransactionType type,
                                  Transaction relatedTransaction);

    Transaction createTransaction(Wallet wallet, BigDecimal amount, TransactionType type);
}
