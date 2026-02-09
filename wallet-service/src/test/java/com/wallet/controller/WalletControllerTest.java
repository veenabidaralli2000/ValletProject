package com.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.dto.WalletOperationRequest;
import com.wallet.entity.Wallet;
import com.wallet.enums.OperationType;
import com.wallet.exception.InsufficientFundsException;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletService walletService;

    @Test
    void deposit_returns200WithNewBalance() throws Exception {
        UUID walletId = UUID.randomUUID();
        WalletOperationRequest req = new WalletOperationRequest();
        req.setWalletId(walletId);
        req.setOperationType(OperationType.DEPOSIT);
        req.setAmount(new BigDecimal("50.00"));

        when(walletService.applyOperation(eq(walletId), eq(OperationType.DEPOSIT), any()))
                .thenReturn(new Wallet(walletId, new BigDecimal("150.00")));

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.balance").value(150.00));
    }

    @Test
    void invalidJson_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void missingFields_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void invalidOperationType_returns400() throws Exception {
        String body = "{\"walletId\":\"" + UUID.randomUUID() + "\","
                + "\"operationType\":\"TRANSFER\",\"amount\":10.00}";

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("TRANSFER")));
    }

    @Test
    void negativeAmount_returns400() throws Exception {
        String body = "{\"walletId\":\"" + UUID.randomUUID() + "\","
                + "\"operationType\":\"DEPOSIT\",\"amount\":-5.00}";

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void walletNotFound_returns404() throws Exception {
        UUID walletId = UUID.randomUUID();
        WalletOperationRequest req = new WalletOperationRequest();
        req.setWalletId(walletId);
        req.setOperationType(OperationType.WITHDRAW);
        req.setAmount(new BigDecimal("10.00"));

        when(walletService.applyOperation(any(), any(), any()))
                .thenThrow(new WalletNotFoundException(walletId));

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Wallet Not Found"));
    }

    @Test
    void insufficientFunds_returns400() throws Exception {
        UUID walletId = UUID.randomUUID();
        WalletOperationRequest req = new WalletOperationRequest();
        req.setWalletId(walletId);
        req.setOperationType(OperationType.WITHDRAW);
        req.setAmount(new BigDecimal("1000000.00"));

        when(walletService.applyOperation(any(), any(), any()))
                .thenThrow(new InsufficientFundsException(walletId, BigDecimal.ZERO, new BigDecimal("1000000.00")));

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient Funds"));
    }

    @Test
    void getBalance_returns200() throws Exception {
        UUID walletId = UUID.randomUUID();
        when(walletService.getWallet(walletId))
                .thenReturn(new Wallet(walletId, new BigDecimal("42.00")));

        mockMvc.perform(get("/api/v1/wallets/{id}", walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(42.00));
    }

    @Test
    void getBalance_invalidUUID_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}
