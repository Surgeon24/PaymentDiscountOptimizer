package m.ermolaev.discounts.model;

import java.math.BigDecimal;

public class PaymentMethod {
    private String id;
    private int discount;
    private BigDecimal limit;

    public PaymentMethod() {
    }

    public PaymentMethod(String id, int discount, BigDecimal limit){
        this.id = id;
        this.discount = discount;
        this.limit = limit;
    }
    public String getId() {
        return this.id;
    }

    public int getDiscount() {
        return this.discount;
    }

    public BigDecimal getLimit() {
        return this.limit;
    }

    public void setLimit(BigDecimal limit) {
        this.limit = limit;
    }
}
