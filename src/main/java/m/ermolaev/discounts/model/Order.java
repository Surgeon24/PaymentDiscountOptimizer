package m.ermolaev.discounts.model;

import java.math.BigDecimal;
import java.util.List;

public class Order {
    private String id;
    private BigDecimal value;
    private List<String> promotions;

    public Order() {
    }

    public Order(String id, BigDecimal value, List<String> promotions) {
        this.id = id;
        this.value = value;
        this.promotions = promotions;
    }

    public String getId() {
        return this.id;
    }

    public BigDecimal getValue() {
        return this.value;
    }

    public List<String> getPromotions() {
        return this.promotions;
    }
}