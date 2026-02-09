package com.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wallet.enums.OperationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class WalletOperationRequest {

    @NotNull(message = "walletId is required")
    @JsonProperty("walletId")
    private UUID walletId;

    @NotNull(message = "operationType is required")
    @JsonProperty("operationType")
    private OperationType operationType;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    @Digits(integer = 17, fraction = 2, message = "amount must have at most 2 decimal places")
    @JsonProperty("amount")
    private BigDecimal amount;

    public WalletOperationRequest() {
    }

    public UUID getWalletId() {
        return walletId;
    }

    public void setWalletId(UUID walletId) {
        this.walletId = walletId;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
