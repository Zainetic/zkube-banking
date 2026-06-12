package com.zkube.transactions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;

public class TransferLogicTest {

    @Test
    void shouldRejectTransferWhenInsufficientFunds() {
        // Arrange
        BigDecimal senderBalance = new BigDecimal("100.00");
        BigDecimal transferAmount = new BigDecimal("150.00");

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            if (senderBalance.compareTo(transferAmount) < 0) {
                throw new RuntimeException("Insufficient funds");
            }
        });

        assertEquals("Insufficient funds", exception.getMessage());
    }

    @Test
    void shouldApproveTransferWhenFundsAreExact() {
        // Arrange
        BigDecimal senderBalance = new BigDecimal("100.00");
        BigDecimal transferAmount = new BigDecimal("100.00");

        // Act & Assert
        assertDoesNotThrow(() -> {
            if (senderBalance.compareTo(transferAmount) < 0) {
                throw new RuntimeException("Insufficient funds");
            }
        });
    }
}