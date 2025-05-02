package com.challenge.wallet.repository;

import com.challenge.wallet.bean.HistoricalBalanceBean;
import com.challenge.wallet.model.Transaction;

import java.time.LocalDateTime;
import java.util.UUID;

public interface TransactionRepository {

    Transaction save(Transaction transaction);

    HistoricalBalanceBean getHistoricalBalance(UUID walletId, LocalDateTime at);
}
