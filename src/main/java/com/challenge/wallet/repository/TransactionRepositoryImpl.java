package com.challenge.wallet.repository;

import com.challenge.wallet.bean.HistoricalBalanceBean;
import com.challenge.wallet.model.Transaction;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class TransactionRepositoryImpl implements TransactionRepository, PanacheRepositoryBase<Transaction, UUID> {

    private static final String HISTORICAL_BALANCE_HQL = """
            select new com.challenge.wallet.bean.HistoricalBalanceBean(
              coalesce(sum(case when operation = 'DEBIT' then -amount else amount end), 0)
              ,:at
            )
            from Transaction
            where wallet.id = :walletId
            and createdAt <= :at
            """;

    @Override
    public Transaction save(Transaction transaction) {
        this.persistAndFlush(transaction);
        return transaction;
    }

    @Override
    public HistoricalBalanceBean getHistoricalBalance(UUID walletId, LocalDateTime at) {
        return getEntityManager()
                .createQuery(HISTORICAL_BALANCE_HQL, HistoricalBalanceBean.class)
                .setParameter("walletId", walletId)
                .setParameter("at", at)
                .getSingleResult();
    }
}
