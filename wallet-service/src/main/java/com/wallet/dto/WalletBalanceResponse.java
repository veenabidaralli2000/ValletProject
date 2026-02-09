package com.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class WalletBalanceResponse {

    private UUID walletId;
    private BigDecimal balance;

    public WalletBalanceResponse() {
    }

    public WalletBalanceResponse(UUID walletId, BigDecimal balance) {
        this.walletId = walletId;
        this.balance = balance;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public void setWalletId(UUID walletId) {
        this.walletId = walletId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
