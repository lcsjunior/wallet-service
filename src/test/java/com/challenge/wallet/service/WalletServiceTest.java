package com.challenge.wallet.service;

import com.challenge.wallet.bean.HistoricalBalanceBean;
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
import com.challenge.wallet.model.Transaction;
import com.challenge.wallet.model.TransactionType;
import com.challenge.wallet.model.Wallet;
import com.challenge.wallet.repository.WalletRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static com.challenge.wallet.constants.Constants.ONE_CENT;
import static com.challenge.wallet.constants.Constants.ONE_TRILLION;
import static com.challenge.wallet.constants.TestConstants.INVALID_CURRENCY_SCALE;
import static com.challenge.wallet.constants.TestConstants.NIL_UUID;
import static com.challenge.wallet.constants.TestConstants.TEST_MAX_DATE;
import static com.challenge.wallet.constants.TestConstants.TEST_UUID_3;
import static com.challenge.wallet.factory.WalletTestFactory.createHistoricalBalanceQuery;
import static com.challenge.wallet.factory.WalletTestFactory.createWallet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class WalletServiceTest {

    @Inject
    WalletService walletService;

    @InjectMock
    WalletRepository walletRepository;

    @InjectMock
    TransactionService transactionService;

    private static Wallet walletMock = mock(Wallet.class);

    private static Transaction transactionMock = mock(Transaction.class);

    private static HistoricalBalanceBean historicalBalanceBeanMock = mock(HistoricalBalanceBean.class);

    @BeforeAll
    static void setup() {
        when(walletMock.getId()).thenReturn(NIL_UUID);
        when(walletMock.getBalance()).thenReturn(ONE_CENT);
        when(walletMock.getUpdatedAt()).thenReturn(TEST_MAX_DATE);

        when(historicalBalanceBeanMock.balance()).thenReturn(ONE_CENT);
        when(historicalBalanceBeanMock.at()).thenReturn(TEST_MAX_DATE);
    }

    @Test
    void shouldCreateWalletSuccessfully() {
        when(walletRepository.save(any(Wallet.class))).thenReturn(walletMock);
        CreateWalletResponse createWalletResponse = walletService.createWallet();
        assertThat(createWalletResponse.walletId())
                .isNotNull()
                .isInstanceOf(UUID.class);
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void shouldThrowExceptionWhenWalletIsNotFound() {
        assertThatThrownBy(() ->
                walletService.getBalance(NIL_UUID))
                .isInstanceOf(WalletNotFoundException.class);
        verify(walletRepository, times(1)).getWallet(any(UUID.class));
    }

    @Test
    void shouldReturnBalanceForWallet() {
        when(walletRepository.getWallet(any(UUID.class))).thenReturn(Optional.of(walletMock));
        BalanceResponse balanceResponse = walletService.getBalance(NIL_UUID);
        assertThat(balanceResponse.balance())
                .isNotNull()
                .isEqualTo(ONE_CENT);
        assertThat(balanceResponse.updatedAt())
                .isNotNull()
                .isEqualTo(TEST_MAX_DATE);
        verify(walletRepository, times(1)).getWallet(any(UUID.class));
    }

    @Test
    void shouldReturnHistoricalBalanceForWallet() {
        when(walletRepository.getWallet(any(UUID.class))).thenReturn(Optional.of(walletMock));
        when(transactionService.getHistoricalBalance(any(UUID.class), any(HistoricalBalanceQuery.class)))
                .thenReturn(historicalBalanceBeanMock);
        HistoricalBalanceResponse historicalBalanceResponse =
                walletService.getHistoricalBalance(NIL_UUID, createHistoricalBalanceQuery());
        assertThat(historicalBalanceResponse.balance())
                .isNotNull()
                .isEqualTo(ONE_CENT);
        assertThat(historicalBalanceResponse.at())
                .isNotNull()
                .isEqualTo(TEST_MAX_DATE);
        verify(walletRepository, times(1)).getWallet(any(UUID.class));
        verify(transactionService, times(1))
                .getHistoricalBalance(any(UUID.class), any(HistoricalBalanceQuery.class));
    }

    @Test
    void shouldThrowExceptionWhenInsufficientAmountOnDeposit() {
        assertThatThrownBy(() ->
                walletService.deposit(new DepositRequest(NIL_UUID, BigDecimal.ZERO)))
                .isInstanceOf(InsufficientAmountException.class);
    }

    @Test
    void shouldThrowExceptionWhenInvalidDecimalScaleAmountOnDeposit() {
        assertThatThrownBy(() ->
                walletService.deposit(new DepositRequest(NIL_UUID, INVALID_CURRENCY_SCALE)))
                .isInstanceOf(DecimalScaleException.class);
    }

    @Test
    void shouldIncreaseBalanceOnDeposit() {
        Wallet wallet = createWallet();
        when(walletRepository.getWallet(any(UUID.class))).thenReturn(Optional.of(wallet));
        walletService.deposit(new DepositRequest(NIL_UUID, ONE_TRILLION));
        assertThat(wallet.getBalance())
                .isNotNull()
                .isEqualByComparingTo(ONE_TRILLION);
        verify(walletRepository, times(1)).getWallet(any(UUID.class));
        verify(transactionService, times(1))
                .createTransaction(any(Wallet.class),
                        any(BigDecimal.class), any(TransactionType.class));
    }

    @Test
    void shouldThrowExceptionWhenInsufficientAmountOnWithdraw() {
        assertThatThrownBy(() ->
                walletService.withdraw(new WithdrawRequest(NIL_UUID, BigDecimal.ZERO)))
                .isInstanceOf(InsufficientAmountException.class);
    }

    @Test
    void shouldThrowExceptionWhenInvalidDecimalScaleAmountOnWithdraw() {
        assertThatThrownBy(() ->
                walletService.withdraw(new WithdrawRequest(NIL_UUID, INVALID_CURRENCY_SCALE)))
                .isInstanceOf(DecimalScaleException.class);
    }

    @Test
    void shouldThrowExceptionWhenInsufficientFunds() {
        Wallet wallet = createWallet();
        when(walletRepository.getWallet(any(UUID.class))).thenReturn(Optional.of(wallet));
        assertThatThrownBy(() ->
                walletService.withdraw(new WithdrawRequest(NIL_UUID, ONE_CENT)))
                .isInstanceOf(InsufficientFundsException.class);
        verify(walletRepository, times(1)).getWallet(any(UUID.class));
    }

    @Test
    void shouldDecreaseBalanceOnWithdraw() {
        BigDecimal amount = ONE_CENT.multiply(BigDecimal.valueOf(2)).add(ONE_TRILLION);
        Wallet wallet = createWallet();
        when(walletRepository.getWallet(any(UUID.class))).thenReturn(Optional.of(wallet));
        walletService.deposit(new DepositRequest(NIL_UUID, amount));
        walletService.withdraw(new WithdrawRequest(NIL_UUID, ONE_CENT));
        walletService.withdraw(new WithdrawRequest(NIL_UUID, ONE_TRILLION));
        walletService.withdraw(new WithdrawRequest(NIL_UUID, ONE_CENT));
        assertThat(wallet.getBalance())
                .isNotNull()
                .isEqualByComparingTo(BigDecimal.ZERO);
        verify(walletRepository, times(4)).getWallet(any(UUID.class));
        verify(transactionService, times(4))
                .createTransaction(any(Wallet.class),
                        any(BigDecimal.class), any(TransactionType.class));
    }

    @Test
    void shouldThrowExceptionWhenSameWalletTransfer() {
        assertThatThrownBy(() ->
                walletService.transfer(new TransferRequest(NIL_UUID, NIL_UUID, ONE_CENT)))
                .isInstanceOf(SameWalletException.class);
    }

    @Test
    void shouldThrowExceptionWhenInsufficientAmountOnTransfer() {
        assertThatThrownBy(() ->
                walletService.transfer(new TransferRequest(NIL_UUID, TEST_UUID_3, BigDecimal.ZERO)))
                .isInstanceOf(InsufficientAmountException.class);
    }

    @Test
    void shouldThrowExceptionWhenInvalidDecimalScaleAmountOnTransfer() {
        assertThatThrownBy(() ->
                walletService.transfer(new TransferRequest(NIL_UUID, TEST_UUID_3, INVALID_CURRENCY_SCALE)))
                .isInstanceOf(DecimalScaleException.class);
    }

    @Test
    void shouldThrowExceptionWhenInsufficientFundsOnTransfer() {
        Wallet wallet = createWallet();
        when(walletRepository.getWallet(any(UUID.class))).thenReturn(Optional.of(wallet));
        assertThatThrownBy(() ->
                walletService.transfer(new TransferRequest(NIL_UUID, TEST_UUID_3, ONE_CENT)))
                .isInstanceOf(InsufficientFundsException.class);
        verify(walletRepository, times(1)).getWallet(any(UUID.class));
    }

    @Test
    void shouldIncreaseBalanceOnTransfer() {
        Wallet wallet = createWallet();
        when(walletRepository.getWallet(any(UUID.class))).thenReturn(Optional.of(wallet));
        when(transactionService.createTransaction(any(Wallet.class), any(BigDecimal.class),
                any(TransactionType.class))).thenReturn(transactionMock);
        walletService.deposit(new DepositRequest(NIL_UUID, ONE_TRILLION));
        walletService.transfer(new TransferRequest(NIL_UUID, TEST_UUID_3, ONE_TRILLION));
        assertThat(wallet.getBalance())
                .isNotNull()
                .isEqualByComparingTo(ONE_TRILLION);
        verify(walletRepository, times(3)).getWallet(any(UUID.class));
        verify(transactionService, times(2))
                .createTransaction(any(Wallet.class),
                        any(BigDecimal.class), any(TransactionType.class));
        verify(transactionService, times(1))
                .createTransaction(any(Wallet.class), any(BigDecimal.class),
                        any(TransactionType.class), any(Transaction.class));
    }
}
