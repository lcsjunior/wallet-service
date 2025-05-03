package com.challenge.wallet.service;

import com.challenge.wallet.bean.HistoricalBalanceBean;
import com.challenge.wallet.dto.HistoricalBalanceQuery;
import com.challenge.wallet.exception.DatetimeFormatException;
import com.challenge.wallet.model.OperationType;
import com.challenge.wallet.model.Transaction;
import com.challenge.wallet.model.TransactionType;
import com.challenge.wallet.model.Wallet;
import com.challenge.wallet.repository.TransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@ApplicationScoped
public class TransactionServiceImpl implements TransactionService {

    @Inject
    TransactionRepository transactionRepository;

    @Override
    public Transaction createTransaction(Wallet wallet, OperationType operation,
                                         BigDecimal amount, TransactionType type, Transaction relatedTransaction) {
        return transactionRepository.save(new Transaction(wallet, operation, amount, type, relatedTransaction));
    }

    @Override
    public HistoricalBalanceBean getHistoricalBalance(UUID walletId, HistoricalBalanceQuery query) {
        LocalDateTime atDateTime = parseIsoDateTime(query.at());
        return transactionRepository.getHistoricalBalance(walletId, atDateTime);
    }

    private LocalDateTime parseIsoDateTime(String dateTime) {
        try {
            return LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new DatetimeFormatException();
        }
    }
}
