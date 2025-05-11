package m.ermolaev.discounts.model;

import java.math.BigDecimal;

public class PaymentResult {
    private String methodId;
    private BigDecimal amount;

    public PaymentResult(String methodId, BigDecimal amount) {
        this.methodId = methodId;
        this.amount = amount;
    }

    public String getMethodId() {
        return this.methodId;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }
    // TODO: toString()
}
