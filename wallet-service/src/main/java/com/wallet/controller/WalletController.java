package com.wallet.controller;

import com.wallet.dto.CreateWalletRequest;
import com.wallet.dto.WalletBalanceResponse;
import com.wallet.dto.WalletOperationRequest;
import com.wallet.entity.Wallet;
import com.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/wallet")
    public ResponseEntity<WalletBalanceResponse> applyOperation(@Valid @RequestBody WalletOperationRequest request) {
        Wallet wallet = walletService.applyOperation(
                request.getWalletId(),
                request.getOperationType(),
                request.getAmount()
        );
        return ResponseEntity.ok(new WalletBalanceResponse(wallet.getId(), wallet.getBalance()));
    }

    @GetMapping("/wallets/{walletId}")
    public ResponseEntity<WalletBalanceResponse> getBalance(@PathVariable("walletId") UUID walletId) {
        Wallet wallet = walletService.getWallet(walletId);
        return ResponseEntity.ok(new WalletBalanceResponse(wallet.getId(), wallet.getBalance()));
    }

    // Helper used by the UI/demo; not part of the core assignment.
    @PostMapping("/wallets")
    public ResponseEntity<WalletBalanceResponse> createWallet(@RequestBody(required = false) CreateWalletRequest request) {
        CreateWalletRequest req = request != null ? request : new CreateWalletRequest();
        Wallet wallet = walletService.createWallet(req.getWalletId(), req.getInitialBalance());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new WalletBalanceResponse(wallet.getId(), wallet.getBalance()));
    }
}
