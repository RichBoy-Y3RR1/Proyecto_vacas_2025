package com.example.backend.services;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class PurchaseServiceTest {
    private final PurchaseService svc = new PurchaseService();

    @Test
    public void testComputeFees_globalOnly() {
        PurchaseService.Fees fees = svc.computeFees(new BigDecimal("100.00"), new BigDecimal("15.00"), null);
        assertEquals(new BigDecimal("15.00"), fees.platformFee().setScale(2));
        assertEquals(new BigDecimal("85.00"), fees.companyAmount().setScale(2));
        assertEquals(new BigDecimal("15.00"), fees.usedPercent().setScale(2));
    }

    @Test
    public void testComputeFees_companyLowerThanGlobal() {
        PurchaseService.Fees fees = svc.computeFees(new BigDecimal("200.00"), new BigDecimal("15.00"), new BigDecimal("10.00"));
        assertEquals(new BigDecimal("20.00"), fees.platformFee().setScale(2));
        assertEquals(new BigDecimal("180.00"), fees.companyAmount().setScale(2));
        assertEquals(new BigDecimal("10.00"), fees.usedPercent().setScale(2));
    }

    @Test
    public void testComputeFees_companyHigherThanGlobal() {
        PurchaseService.Fees fees = svc.computeFees(new BigDecimal("50.00"), new BigDecimal("12.00"), new BigDecimal("20.00"));
        
        assertEquals(new BigDecimal("6.00"), fees.platformFee().setScale(2));
        assertEquals(new BigDecimal("44.00"), fees.companyAmount().setScale(2));
        assertEquals(new BigDecimal("12.00"), fees.usedPercent().setScale(2));
    }
}
