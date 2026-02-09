package com.wallet.service;

import com.wallet.entity.Wallet;
import com.wallet.enums.OperationType;
import com.wallet.exception.InsufficientFundsException;
import com.wallet.exception.WalletAlreadyExistsException;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletService walletService;

    private UUID walletId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        wallet = new Wallet(walletId, new BigDecimal("100.00"));
    }

    @Test
    void deposit_increasesBalance() {
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.saveAndFlush(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));

        Wallet result = walletService.applyOperation(walletId, OperationType.DEPOSIT, new BigDecimal("50.00"));

        assertThat(result.getBalance()).isEqualByComparingTo("150.00");
    }

    @Test
    void withdraw_decreasesBalance() {
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.saveAndFlush(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));

        Wallet result = walletService.applyOperation(walletId, OperationType.WITHDRAW, new BigDecimal("40.00"));

        assertThat(result.getBalance()).isEqualByComparingTo("60.00");
    }

    @Test
    void withdraw_insufficientFunds_throws() {
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() ->
                walletService.applyOperation(walletId, OperationType.WITHDRAW, new BigDecimal("200.00"))
        ).isInstanceOf(InsufficientFundsException.class);

        verify(walletRepository, never()).saveAndFlush(any());
    }

    @Test
    void operation_walletNotFound_throws() {
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                walletService.applyOperation(walletId, OperationType.DEPOSIT, new BigDecimal("10.00"))
        ).isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    void getWallet_notFound_throws() {
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> walletService.getWallet(walletId))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    void createWallet_duplicateId_throws() {
        when(walletRepository.existsById(walletId)).thenReturn(true);
        assertThatThrownBy(() -> walletService.createWallet(walletId, BigDecimal.ZERO))
                .isInstanceOf(WalletAlreadyExistsException.class);
    }

    @Test
    void createWallet_negativeBalance_throws() {
        when(walletRepository.existsById(any())).thenReturn(false);
        assertThatThrownBy(() -> walletService.createWallet(walletId, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
