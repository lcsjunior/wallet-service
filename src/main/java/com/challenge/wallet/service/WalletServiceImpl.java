package com.challenge.wallet.service;

import com.challenge.wallet.bean.HistoricalBalanceBean;
import com.challenge.wallet.constants.Constants;
import com.challenge.wallet.dto.BalanceResponse;
import com.challenge.wallet.dto.CreateWalletResponse;
import com.challenge.wallet.dto.DepositRequest;
import com.challenge.wallet.dto.HistoricalBalanceQuery;
import com.challenge.wallet.dto.HistoricalBalanceResponse;
import com.challenge.wallet.dto.TransferRequest;
import com.challenge.wallet.dto.WithdrawRequest;
import com.challenge.wallet.exception.InsufficientAmountException;
import com.challenge.wallet.exception.InsufficientFundsException;
import com.challenge.wallet.exception.InvalidDecimalScaleException;
import com.challenge.wallet.exception.SameWalletTransferException;
import com.challenge.wallet.exception.WalletNotFoundException;
import com.challenge.wallet.model.Transaction;
import com.challenge.wallet.model.TransactionType;
import com.challenge.wallet.model.Wallet;
import com.challenge.wallet.repository.WalletRepository;
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
        Wallet wallet = walletRepository.save(new Wallet());
        return CreateWalletResponse.from(wallet);
    }

    @Override
    public BalanceResponse getBalance(UUID walletId) {
        Wallet wallet = getWallet(walletId);
        return BalanceResponse.from(wallet);
    }

    @Override
    public HistoricalBalanceResponse getHistoricalBalance(UUID walletId, HistoricalBalanceQuery query) {
        checkWalletExists(walletId);
        HistoricalBalanceBean historicalBalanceBean = transactionService.getHistoricalBalance(walletId, query);
        return HistoricalBalanceResponse.from(historicalBalanceBean);
    }

    @Transactional
    @Override
    public void deposit(DepositRequest request) {
        BigDecimal amount = request.amount();
        validateAmount(amount);

        Wallet wallet = getWallet(request.walletId());
        credit(wallet, amount);
        transactionService.createTransaction(wallet, amount, TransactionType.CREDIT);
    }

    @Transactional
    @Override
    public void withdraw(WithdrawRequest request) {
        BigDecimal amount = request.amount();
        validateAmount(amount);

        Wallet wallet = getWallet(request.walletId());
        debit(wallet, amount);
        transactionService.createTransaction(wallet, amount, TransactionType.DEBIT);
    }

    @Transactional
    @Override
    public void transfer(TransferRequest request) {
        BigDecimal amount = request.amount();
        validateAmount(amount);

        if (request.fromWalletId().equals(request.toWalletId())) {
            throw new SameWalletTransferException();
        }
        Wallet walletFrom = getWallet(request.fromWalletId());
        Wallet walletTo = getWallet(request.toWalletId());

        debit(walletFrom, amount);
        Transaction transactionFrom = transactionService.createTransaction(walletFrom, amount, TransactionType.DEBIT);

        credit(walletTo, amount);
        transactionService.createTransaction(walletTo, amount, TransactionType.CREDIT, transactionFrom);
    }

    private void debit(Wallet wallet, BigDecimal amount) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

    }

    private void credit(Wallet wallet, BigDecimal amount) {
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
    }

    private Wallet getWallet(UUID walletId) {
        return walletRepository.getWallet(walletId).orElseThrow(WalletNotFoundException::new);
    }

    private void checkWalletExists(UUID walletId) {
        walletRepository.getWallet(walletId);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount.compareTo(Constants.ONE_CENT) < 0) {
            throw new InsufficientAmountException();
        }
        if (amount.scale() > 2) {
            throw new InvalidDecimalScaleException();
        }
    }
}
