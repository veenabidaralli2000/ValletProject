package com.wallet.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.dto.WalletOperationRequest;
import com.wallet.entity.Wallet;
import com.wallet.enums.OperationType;
import com.wallet.repository.WalletRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WalletIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRepository walletRepository;

    private UUID walletId;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        walletRepository.saveAndFlush(new Wallet(walletId, new BigDecimal("100.00")));
    }

    @AfterEach
    void tearDown() {
        walletRepository.deleteById(walletId);
    }

    @Test
    void fullDepositFlow_persistsNewBalance() throws Exception {
        WalletOperationRequest req = new WalletOperationRequest();
        req.setWalletId(walletId);
        req.setOperationType(OperationType.DEPOSIT);
        req.setAmount(new BigDecimal("25.50"));

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(125.50));

        Wallet reloaded = walletRepository.findById(walletId).orElseThrow();
        assertThat(reloaded.getBalance()).isEqualByComparingTo("125.50");
    }

    @Test
    void withdrawBelowZero_returns400_andBalanceUnchanged() throws Exception {
        WalletOperationRequest req = new WalletOperationRequest();
        req.setWalletId(walletId);
        req.setOperationType(OperationType.WITHDRAW);
        req.setAmount(new BigDecimal("999.00"));

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient Funds"));

        Wallet reloaded = walletRepository.findById(walletId).orElseThrow();
        assertThat(reloaded.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void getBalance_returnsCurrentBalance() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/{id}", walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    @Test
    void getBalance_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Wallet Not Found"));
    }
}
