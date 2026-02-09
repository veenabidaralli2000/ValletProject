package com.wallet.exception;

import java.util.UUID;

public class WalletAlreadyExistsException extends RuntimeException {

    public WalletAlreadyExistsException(UUID walletId) {
        super("Wallet already exists: " + walletId);
    }
}
