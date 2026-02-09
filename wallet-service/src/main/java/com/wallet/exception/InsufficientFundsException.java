package com.wallet.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(UUID walletId, BigDecimal balance, BigDecimal requested) {
        super(String.format(
                "Insufficient funds for wallet %s. Balance: %s, requested: %s",
                walletId, balance, requested));
    }
}
