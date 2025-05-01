package com.challenge.wallet.service;

import com.challenge.wallet.constants.Constants;
import com.challenge.wallet.dto.BalanceResponse;
import com.challenge.wallet.dto.CreateWalletResponse;
import com.challenge.wallet.dto.DepositRequest;
import com.challenge.wallet.dto.HistoricalBalanceQuery;
import com.challenge.wallet.dto.TransferRequest;
import com.challenge.wallet.dto.WithdrawRequest;
import com.challenge.wallet.exception.InsufficientAmountException;
import com.challenge.wallet.exception.InsufficientFundsException;
import com.challenge.wallet.exception.InvalidDatetimeFormatException;
import com.challenge.wallet.exception.SameWalletTransferException;
import com.challenge.wallet.model.Transaction;
import com.challenge.wallet.model.TransactionType;
import com.challenge.wallet.model.Wallet;
import com.challenge.wallet.repository.WalletRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
        Wallet wallet = walletRepository.getWallet(walletId);
        return BalanceResponse.from(wallet);
    }

    @Override
    public Object getHistoricalBalance(UUID walletId, HistoricalBalanceQuery query) {
        LocalDateTime atDateTime = parseIsoDateTime(query.at());
        checkWalletExists(walletId);
        return null;
    }

    @Transactional
    @Override
    public void deposit(DepositRequest request) {
        BigDecimal amount = request.amount();
        validateAmount(amount);

        Wallet wallet = walletRepository.getWallet(request.walletId());
        credit(wallet, amount);
        transactionService.createTransaction(wallet, amount, TransactionType.CREDIT);
    }

    @Transactional
    @Override
    public void withdraw(WithdrawRequest request) {
        BigDecimal amount = request.amount();
        validateAmount(amount);

        Wallet wallet = walletRepository.getWallet(request.walletId());
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
        Wallet walletFrom = walletRepository.getWallet(request.fromWalletId());
        Wallet walletTo = walletRepository.getWallet(request.toWalletId());

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

    private LocalDateTime parseIsoDateTime(String dateTime) {
        try {
            return LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new InvalidDatetimeFormatException();
        }
    }

    private void checkWalletExists(UUID walletId) {
        walletRepository.getWallet(walletId);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount.compareTo(Constants.ONE_CENT) < 0) {
            throw new InsufficientAmountException();
        }
    }
}
