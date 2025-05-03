package com.challenge.wallet.service;

import com.challenge.wallet.bean.HistoricalBalanceBean;
import com.challenge.wallet.dto.BalanceResponse;
import com.challenge.wallet.dto.CreateWalletResponse;
import com.challenge.wallet.dto.DepositRequest;
import com.challenge.wallet.dto.HistoricalBalanceQuery;
import com.challenge.wallet.dto.HistoricalBalanceResponse;
import com.challenge.wallet.dto.TransferRequest;
import com.challenge.wallet.dto.WithdrawRequest;
import com.challenge.wallet.exception.WalletNotFoundException;
import com.challenge.wallet.model.OperationType;
import com.challenge.wallet.model.Transaction;
import com.challenge.wallet.model.TransactionType;
import com.challenge.wallet.model.Wallet;
import com.challenge.wallet.repository.WalletRepository;
import com.challenge.wallet.validator.WalletValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@ApplicationScoped
public class WalletServiceImpl implements WalletService {

    @Inject
    WalletRepository walletRepository;

    @Inject
    TransactionService transactionService;

    @Transactional
    @Override
    public CreateWalletResponse createWallet() {
        Wallet wallet = saveWallet(new Wallet());
        return CreateWalletResponse.from(wallet);
    }

    @Override
    public BalanceResponse retrieveBalance(UUID walletId) {
        Wallet wallet = retrieveWallet(walletId);
        return BalanceResponse.from(wallet);
    }

    @Override
    public HistoricalBalanceResponse retrieveHistoricalBalance(UUID walletId, HistoricalBalanceQuery query) {
        retrieveWallet(walletId);
        HistoricalBalanceBean historicalBalanceBean = transactionService.getHistoricalBalance(walletId, query);
        return HistoricalBalanceResponse.from(historicalBalanceBean);
    }

    @Transactional
    @Override
    public void deposit(DepositRequest request) {
        BigDecimal amount = request.amount();
        Wallet wallet = retrieveWallet(request.walletId());
        wallet.credit(amount);

        transactionService.createTransaction(
                wallet, OperationType.CREDIT, amount, TransactionType.DEPOSIT, null);
        saveWallet(wallet);
    }

    @Transactional
    @Override
    public void withdraw(WithdrawRequest request) {
        BigDecimal amount = request.amount();
        Wallet wallet = retrieveWallet(request.walletId());
        wallet.debit(amount);

        transactionService.createTransaction(
                wallet, OperationType.DEBIT, amount, TransactionType.WITHDRAW, null);
        saveWallet(wallet);
    }

    @Transactional
    @Override
    public void transfer(TransferRequest request) {
        WalletValidator.validateSameWallet(request.fromWalletId(), request.toWalletId());

        BigDecimal amount = request.amount();
        Wallet walletFrom = retrieveWallet(request.fromWalletId());
        Wallet walletTo = retrieveWallet(request.toWalletId());

        walletFrom.debit(amount);
        walletTo.credit(amount);

        Transaction relatedTransaction = transactionService.createTransaction(
                walletFrom, OperationType.DEBIT, amount, TransactionType.TRANSFER, null);
        transactionService.createTransaction(
                walletTo, OperationType.CREDIT, amount, TransactionType.TRANSFER, relatedTransaction);
        saveWallet(walletFrom);
        saveWallet(walletTo);
    }

    private Wallet saveWallet(Wallet wallet) {
        return walletRepository.save(wallet);
    }

    private Wallet retrieveWallet(UUID walletId) {
        return walletRepository.retrieveWallet(walletId).orElseThrow(WalletNotFoundException::new);
    }
}
