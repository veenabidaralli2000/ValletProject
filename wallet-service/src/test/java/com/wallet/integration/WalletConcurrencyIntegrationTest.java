package com.wallet.integration;

import com.wallet.entity.Wallet;
import com.wallet.enums.OperationType;
import com.wallet.repository.WalletRepository;
import com.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that optimistic locking + Spring Retry correctly processes a high
 * volume of concurrent operations on the same wallet without losing updates
 * and without returning any 5xx error.
 */
@SpringBootTest
@ActiveProfiles("test")
class WalletConcurrencyIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    private UUID walletId;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        walletRepository.save(new Wallet(walletId, new BigDecimal("0.00")));
    }

    @Test
    void concurrentDeposits_allSucceed_andTotalIsCorrect() throws Exception {
        int threads = 32;
        int opsPerThread = 50;
        int totalOps = threads * opsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger failures = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            futures.add(executor.submit(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = 0; i < opsPerThread; i++) {
                    try {
                        walletService.applyOperation(
                                walletId, OperationType.DEPOSIT, new BigDecimal("1.00"));
                    } catch (Exception ex) {
                        failures.incrementAndGet();
                    }
                }
            }));
        }

        start.countDown();
        for (Future<?> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(failures.get())
                .as("no concurrent operation should be left unprocessed")
                .isZero();

        BigDecimal finalBalance = walletRepository.findById(walletId).orElseThrow().getBalance();
        assertThat(finalBalance)
                .as("no lost updates under concurrency")
                .isEqualByComparingTo(new BigDecimal(totalOps));
    }
}
