package com.challenge.wallet.repository;

import com.challenge.wallet.bean.HistoricalBalanceBean;
import com.challenge.wallet.model.Transaction;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class TransactionRepositoryImpl implements TransactionRepository, PanacheRepositoryBase<Transaction, UUID> {

    @Override
    public Transaction save(Transaction transaction) {
        this.persistAndFlush(transaction);
        return transaction;
    }

    @Override
    public HistoricalBalanceBean getHistoricalBalance(UUID walletId, LocalDateTime at) {
        StringBuilder hql = new StringBuilder()
                .append("select new com.challenge.wallet.bean.HistoricalBalanceBean(")
                .append("  coalesce(sum(case when type = 'DEBIT' then -amount else amount end), 0) ")
                .append("  ,:at ")
                .append(") ")
                .append("from Transaction ")
                .append("where wallet.id = :walletId ")
                .append("and createdAt <= :at");
        return getEntityManager()
                .createQuery(hql.toString(), HistoricalBalanceBean.class)
                .setParameter("walletId", walletId)
                .setParameter("at", at)
                .getSingleResult();
    }
}
