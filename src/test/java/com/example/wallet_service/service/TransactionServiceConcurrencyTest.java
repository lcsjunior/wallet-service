package com.example.wallet_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.wallet_service.entity.Wallet;
import com.example.wallet_service.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TransactionServiceConcurrencyTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WalletRepository walletRepository;

    private String userId;

    @BeforeEach
    void setUp() {
        userId = "concurrency-" + UUID.randomUUID();
        walletRepository.saveAndFlush(Wallet.createNew(userId));
    }

    @AfterEach
    void tearDown() {
        walletRepository.findByUserId(userId).ifPresent(walletRepository::delete);
    }

    @Test
    @DisplayName("Não deve permitir saldo negativo sob dois saques concorrentes na mesma carteira")
    void shouldNotAllowNegativeBalanceUnderConcurrentWithdrawals() throws InterruptedException {
        transactionService.deposit(userId, new BigDecimal("100.00"), "seed-" + userId);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        List<Runnable> tasks = List.of(
                () -> attemptWithdrawal(readyLatch, startLatch, successCount, "wd-a-" + userId),
                () -> attemptWithdrawal(readyLatch, startLatch, successCount, "wd-b-" + userId));

        tasks.forEach(executor::submit);
        readyLatch.await();
        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow();
        assertThat(wallet.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(successCount.get()).isEqualTo(1);
    }

    private void attemptWithdrawal(CountDownLatch readyLatch, CountDownLatch startLatch, AtomicInteger successCount,
            String correlationId) {
        readyLatch.countDown();
        try {
            startLatch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        }
        try {
            transactionService.withdraw(userId, new BigDecimal("80.00"), correlationId);
            successCount.incrementAndGet();
        } catch (com.example.wallet_service.service.exception.InsufficientBalanceException ignored) {
        }
    }
}
