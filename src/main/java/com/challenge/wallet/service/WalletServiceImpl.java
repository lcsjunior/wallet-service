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
import com.challenge.wallet.exception.DecimalScaleException;
import com.challenge.wallet.exception.SameWalletException;
import com.challenge.wallet.exception.WalletNotFoundException;
import com.challenge.wallet.model.OperationType;
import com.challenge.wallet.model.Transaction;
import com.challenge.wallet.model.TransactionType;
import com.challenge.wallet.model.Wallet;
import com.challenge.wallet.repository.WalletRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.challenge.wallet.constants.Constants.CURRENCY_DECIMAL_SCALE;

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
        validatePaymentAmount(amount);

        Wallet wallet = getWallet(request.walletId());
        credit(wallet, amount, TransactionType.DEPOSIT, null);
    }

    @Transactional
    @Override
    public void withdraw(WithdrawRequest request) {
        BigDecimal amount = request.amount();
        validatePaymentAmount(amount);

        Wallet wallet = getWallet(request.walletId());
        debit(wallet, amount, TransactionType.WITHDRAW);
    }

    @Transactional
    @Override
    public void transfer(TransferRequest request) {
        validateSameWallet(request.fromWalletId(), request.toWalletId());

        BigDecimal amount = request.amount();
        validatePaymentAmount(amount);

        Wallet walletFrom = getWallet(request.fromWalletId());
        validateAvailableBalance(walletFrom.getBalance(), amount);
        Wallet walletTo = getWallet(request.toWalletId());

        Transaction relatedTransaction = debit(walletFrom, amount, TransactionType.TRANSFER);
        credit(walletTo, amount, TransactionType.TRANSFER, relatedTransaction);
    }

    private Transaction debit(Wallet wallet, BigDecimal amount, TransactionType transactionType) {
        validateAvailableBalance(wallet.getBalance(), amount);
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        return transactionService.createTransaction(
                wallet, OperationType.DEBIT, amount, transactionType, null);
    }

    private void credit(Wallet wallet, BigDecimal amount, TransactionType transactionType,
                        Transaction relatedTransaction) {
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
        transactionService.createTransaction(
                wallet, OperationType.CREDIT, amount, transactionType, relatedTransaction);
    }

    private Wallet getWallet(UUID walletId) {
        return walletRepository.getWallet(walletId).orElseThrow(WalletNotFoundException::new);
    }

    private void checkWalletExists(UUID walletId) {
        walletRepository.getWallet(walletId);
    }

    private void validatePaymentAmount(BigDecimal amount) {
        if (amount.compareTo(Constants.ONE_CENT) < 0) {
            throw new InsufficientAmountException();
        }
        if (amount.scale() > CURRENCY_DECIMAL_SCALE) {
            throw new DecimalScaleException();
        }
    }

    private void validateAvailableBalance(BigDecimal balance, BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }
    }

    private void validateSameWallet(UUID walletId1, UUID walletId2) {
        if (walletId1.equals(walletId2)) {
            throw new SameWalletException();
        }
    }
}
