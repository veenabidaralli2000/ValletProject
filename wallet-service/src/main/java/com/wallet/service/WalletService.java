package com.wallet.service;

import com.wallet.entity.Wallet;
import com.wallet.enums.OperationType;
import com.wallet.exception.InsufficientFundsException;
import com.wallet.exception.WalletAlreadyExistsException;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.repository.WalletRepository;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    // Retries on optimistic-lock conflicts so hot wallets don't 5xx under load.
    // TODO: consider adding an idempotency-key header for safe client retries.
    @Retryable(
            retryFor = {
                    OptimisticLockException.class,
                    OptimisticLockingFailureException.class,
                    ObjectOptimisticLockingFailureException.class
            },
            maxAttemptsExpression = "${wallet.optimistic-lock.max-attempts:50}",
            backoff = @Backoff(
                    delayExpression = "${wallet.optimistic-lock.initial-delay-ms:10}",
                    maxDelayExpression = "${wallet.optimistic-lock.max-delay-ms:200}",
                    multiplierExpression = "${wallet.optimistic-lock.multiplier:2.0}",
                    random = true
            )
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Wallet applyOperation(UUID walletId, OperationType operationType, BigDecimal amount) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));

        BigDecimal newBalance;
        if (operationType == OperationType.DEPOSIT) {
            newBalance = wallet.getBalance().add(amount);
        } else {
            newBalance = wallet.getBalance().subtract(amount);
            if (newBalance.signum() < 0) {
                throw new InsufficientFundsException(walletId, wallet.getBalance(), amount);
            }
        }

        wallet.setBalance(newBalance);
        return walletRepository.saveAndFlush(wallet);
    }

    @Transactional(readOnly = true)
    public Wallet getWallet(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
    }

    @Transactional
    public Wallet createWallet(UUID walletId, BigDecimal initialBalance) {
        UUID id = walletId != null ? walletId : UUID.randomUUID();
        if (walletRepository.existsById(id)) {
            throw new WalletAlreadyExistsException(id);
        }
        BigDecimal balance = initialBalance != null ? initialBalance : BigDecimal.ZERO;
        if (balance.signum() < 0) {
            throw new IllegalArgumentException("initialBalance cannot be negative");
        }
        Wallet saved = walletRepository.save(new Wallet(id, balance));
        log.info("Created wallet {} with initial balance {}", saved.getId(), saved.getBalance());
        return saved;
    }
}
