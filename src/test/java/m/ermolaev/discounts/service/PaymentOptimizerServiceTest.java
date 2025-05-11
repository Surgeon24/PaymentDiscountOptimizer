package m.ermolaev.discounts.service;

import m.ermolaev.discounts.model.Order;
import m.ermolaev.discounts.model.PaymentMethod;
import m.ermolaev.discounts.model.PaymentResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentOptimizerServiceTest {

    private final PaymentOptimizerService service = new PaymentOptimizerService();

    private Map<String, BigDecimal> asMap(List<PaymentResult> results) {
        return results.stream().collect(Collectors.toMap(PaymentResult::getMethodId, PaymentResult::getAmount));
    }

    @Test
    void testFullCardPaymentWithPromo() {
        var orders = List.of(new Order("ORDER1", new BigDecimal("100.00"), List.of("CARD1")));
        var methods = List.of(
                new PaymentMethod("CARD1", 10, new BigDecimal("200.00"))
        );

        var result = asMap(service.optimize(orders, methods));
        assertEquals(new BigDecimal("90.00"), result.get("CARD1"));
    }

    @Test
    void testFullPointsPayment() {
        var orders = List.of(new Order("ORDER1", new BigDecimal("100.00"), null));
        var methods = List.of(
                new PaymentMethod("PUNKTY", 15, new BigDecimal("200.00"))
        );

        var result = asMap(service.optimize(orders, methods));
        assertEquals(new BigDecimal("85.00"), result.get("PUNKTY"));
    }

    @Test
    void testPartialPointsDiscount() {
        var orders = List.of(new Order("ORDER1", new BigDecimal("100.00"), null));
        var methods = List.of(
                new PaymentMethod("PUNKTY", 15, new BigDecimal("20.00")),
                new PaymentMethod("CARD1", 0, new BigDecimal("200.00"))
        );

        var result = asMap(service.optimize(orders, methods));
        // 10% discount, total 90. 20 - by PUNKTY, 70 - by card
        assertEquals(new BigDecimal("20.00"), result.get("PUNKTY"));
        assertEquals(new BigDecimal("70.00"), result.get("CARD1"));
    }

    @Test
    void testCardWithoutPromoNoDiscount() {
        var orders = List.of(new Order("ORDER1", new BigDecimal("100.00"), List.of("CARD2"))); // CARD1 не в промо
        var methods = List.of(
                new PaymentMethod("CARD1", 10, new BigDecimal("200.00"))
        );

        var result = asMap(service.optimize(orders, methods));
        assertEquals(new BigDecimal("100.00"), result.get("CARD1")); // без скидки
    }

    @Test
    void testNotEnoughPointsNoPartialDiscount() {
        var orders = List.of(new Order("ORDER1", new BigDecimal("100.00"), null));
        var methods = List.of(
                new PaymentMethod("PUNKTY", 15, new BigDecimal("5.00")),
                new PaymentMethod("CARD1", 0, new BigDecimal("200.00"))
        );

        var result = asMap(service.optimize(orders, methods));
        assertFalse(result.containsKey("PUNKTY"));
        assertEquals(new BigDecimal("100.00"), result.get("CARD1"));
    }

    @Test
    void testPreferPointsIfEqualDiscount() {
        var orders = List.of(new Order("ORDER1", new BigDecimal("100.00"), List.of("CARD1")));
        var methods = List.of(
                new PaymentMethod("PUNKTY", 10, new BigDecimal("200.00")),
                new PaymentMethod("CARD1", 10, new BigDecimal("200.00"))
        );

        var result = asMap(service.optimize(orders, methods));
        assertEquals(new BigDecimal("90.00"), result.get("PUNKTY")); // при равной скидке — баллы в приоритете
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "100.00, 20.00, 15, 0, 90.00, 20.00, 70.00", // скидка 10%, 20 баллов, 70 картой
            "100.00, 50.00, 15, 0, 90.00, 50.00, 40.00", // больше баллов — больше сэкономили на карте
            "100.00, 9.99, 15, 0, 100.00, 0.00, 100.00"  // баллов < 10% — скидка не применяется
    })
    void testPartialPointsUseMaximum(String orderVal, String pointsAvailable, int pointsDiscount, int cardDiscount,
                                     String expectedTotal, String expectedPoints, String expectedCard) {
        var orders = List.of(new Order("ORDER1", new BigDecimal(orderVal), null));
        var methods = List.of(
                new PaymentMethod("PUNKTY", pointsDiscount, new BigDecimal(pointsAvailable)),
                new PaymentMethod("CARD1", cardDiscount, new BigDecimal("200.00"))
        );

        var result = asMap(service.optimize(orders, methods));

        if (new BigDecimal(pointsAvailable).compareTo(new BigDecimal(orderVal).multiply(BigDecimal.valueOf(0.1))) >= 0) {
            assertEquals(new BigDecimal(expectedPoints), result.get("PUNKTY"));
            assertEquals(new BigDecimal(expectedCard), result.get("CARD1"));
        } else {
            assertFalse(result.containsKey("PUNKTY"));
            assertEquals(new BigDecimal("100.00"), result.get("CARD1"));
        }
    }

}
