package m.ermolaev.discounts.model;

import java.math.BigDecimal;

public class PaymentResult {
    private String methodId;
    private BigDecimal amount;

    public PaymentResult(String methodId, BigDecimal amount) {
        this.methodId = methodId;
        this.amount = amount;
    }

    // TODO: getters and toString()
}
