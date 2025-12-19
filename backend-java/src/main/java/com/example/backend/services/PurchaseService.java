package com.example.backend.services;

import java.math.BigDecimal;


public class PurchaseService {
    public static class Fees {
        private final BigDecimal platformFee;
        private final BigDecimal companyAmount;
        private final BigDecimal usedPercent;

        public Fees(BigDecimal platformFee, BigDecimal companyAmount, BigDecimal usedPercent) {
            this.platformFee = platformFee;
            this.companyAmount = companyAmount;
            this.usedPercent = usedPercent;
        }

        public BigDecimal platformFee() { return platformFee; }
        public BigDecimal companyAmount() { return companyAmount; }
        public BigDecimal usedPercent() { return usedPercent; }
    }

    /**
     * Calcula las comisiones y montos resultantes.
     * usePercent = companyPercent != null ? min(companyPercent, globalPercent) : globalPercent
     */
    public Fees computeFees(BigDecimal price, BigDecimal globalPercent, BigDecimal companyPercent){
        BigDecimal usePercent = globalPercent;
        if (companyPercent != null) usePercent = companyPercent.min(globalPercent);
        BigDecimal platformFee = price.multiply(usePercent).divide(new BigDecimal("100"));
        BigDecimal companyAmount = price.subtract(platformFee);
        return new Fees(platformFee, companyAmount, usePercent);
    }
}
