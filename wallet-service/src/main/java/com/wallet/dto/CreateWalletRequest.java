package com.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class CreateWalletRequest {

    private UUID walletId;
    private BigDecimal initialBalance;

    public CreateWalletRequest() {
    }

    public UUID getWalletId() {
        return walletId;
    }

    public void setWalletId(UUID walletId) {
        this.walletId = walletId;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }
}
