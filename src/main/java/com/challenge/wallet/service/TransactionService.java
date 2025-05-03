package com.challenge.wallet.service;

import com.challenge.wallet.bean.HistoricalBalanceBean;
import com.challenge.wallet.dto.HistoricalBalanceQuery;
import com.challenge.wallet.model.OperationType;
import com.challenge.wallet.model.Transaction;
import com.challenge.wallet.model.TransactionType;
import com.challenge.wallet.model.Wallet;

import java.math.BigDecimal;
import java.util.UUID;

public interface TransactionService {

    Transaction createTransaction(Wallet wallet, OperationType operation,
                                  BigDecimal amount, TransactionType type, Transaction relatedTransaction);

    HistoricalBalanceBean getHistoricalBalance(UUID walletId, HistoricalBalanceQuery query);
}
