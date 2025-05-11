package m.ermolaev.discounts.service;

import m.ermolaev.discounts.model.Order;
import m.ermolaev.discounts.model.PaymentMethod;
import m.ermolaev.discounts.model.PaymentResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class PaymentOptimizerService {

    private static final String LOYALTY_ID = "PUNKTY";
    private static final int LOYALTY_PERCENT = 10;

    public List<PaymentResult> optimize(List<Order> orders, List<PaymentMethod> methods) {
        Map<String, BigDecimal> spentPerMethod = new HashMap<>();
        Map<String, PaymentMethod> methodMap = new HashMap<>();

        // copy limits
        for (PaymentMethod method : methods) {
            methodMap.put(method.getId(), new PaymentMethod(method.getId(), method.getDiscount(), method.getLimit()));
            spentPerMethod.put(method.getId(), BigDecimal.ZERO);
        }

        // process each order
        for (Order order : orders) {
            processOrder(order, methodMap, spentPerMethod);
        }

        // final result
        List<PaymentResult> results = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : spentPerMethod.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                results.add(new PaymentResult(entry.getKey(), entry.getValue().setScale(2, RoundingMode.HALF_UP)));
            }
        }

        return results;
    }

    private void processOrder(Order order, Map<String, PaymentMethod> methodMap, Map<String, BigDecimal> spent) {
        BigDecimal orderValue = order.getValue();
        List<String> promos = order.getPromotions() != null ? order.getPromotions() : new ArrayList<>();

        // options: payment method, discount, sum after discount, terms of usage
        class Option {
            String methodId;
            BigDecimal cost;
            BigDecimal fromLoyalty;
            BigDecimal fromCard;
        }

        List<Option> options = new ArrayList<>();
        System.out.println("order id: " + order.getId());
        // 1. By loyalty points
        PaymentMethod points = methodMap.get(LOYALTY_ID);
        if (points != null && points.getLimit().compareTo(orderValue) >= 0) {
            BigDecimal discounted = applyDiscount(orderValue, points.getDiscount());
            options.add(new Option() {{
                methodId = LOYALTY_ID;
                cost = discounted;
                fromLoyalty = discounted;
                fromCard = BigDecimal.ZERO;
            }});
        }

        // 2. By loyalty points (10%) + card
        if (points != null) {
            BigDecimal tenPercent = orderValue.multiply(BigDecimal.valueOf(0.1)).setScale(2, RoundingMode.HALF_UP);
            if (points.getLimit().compareTo(tenPercent) >= 0) {
                BigDecimal discounted = applyDiscount(orderValue, LOYALTY_PERCENT);
                BigDecimal maxPointsUsable = discounted.min(points.getLimit()); // максимально возможная доля баллами (≠ 10%)

                for (PaymentMethod card : methodMap.values()) {
                    if (!card.getId().equals(LOYALTY_ID)) {
                        BigDecimal remaining = discounted.subtract(maxPointsUsable);
                        if (card.getLimit().compareTo(remaining) >= 0) {
                            options.add(new Option() {{
                                methodId = "COMBO:" + card.getId();
                                cost = discounted;
                                fromLoyalty = maxPointsUsable;
                                fromCard = remaining;
                            }});
                        }
                    }
                }
            }
        }

        // 3. By card with a discount
        for (String promo : promos) {
            PaymentMethod method = methodMap.get(promo);
            if (method != null && method.getLimit().compareTo(orderValue) >= 0) {
                BigDecimal discounted = applyDiscount(orderValue, method.getDiscount());
                options.add(new Option() {{
                    methodId = promo;
                    cost = discounted;
                    fromLoyalty = BigDecimal.ZERO;
                    fromCard = discounted;
                }});
            }
        }

        // 4. Without any discount
        for (PaymentMethod method : methodMap.values()) {
            if (!method.getId().equals(LOYALTY_ID) && method.getLimit().compareTo(orderValue) >= 0) {
                options.add(new Option() {{
                    methodId = method.getId();
                    cost = orderValue;
                    fromLoyalty = BigDecimal.ZERO;
                    fromCard = orderValue;
                }});
            }
        }

        // Choose best option
        options.sort(Comparator.comparing(o -> o.cost));
        Option best = options.get(0);
        //print all options to analise the logic
//        for (Option option : options){
//            System.out.println("option 1: " + option.methodId);
//            System.out.println(option.cost + "\t" + option.fromCard + "\t" + option.fromLoyalty);
//        }

        // update status of cards
        if (best.fromLoyalty.compareTo(BigDecimal.ZERO) > 0) {
            PaymentMethod p = methodMap.get(LOYALTY_ID);
            p.setLimit(p.getLimit().subtract(best.fromLoyalty));
            spent.merge(LOYALTY_ID, best.fromLoyalty, BigDecimal::add);
        }

        if (best.fromCard.compareTo(BigDecimal.ZERO) > 0) {
            String cardId = best.methodId.startsWith("COMBO:") ? best.methodId.substring(6) : best.methodId;
            PaymentMethod c = methodMap.get(cardId);
            c.setLimit(c.getLimit().subtract(best.fromCard));
            spent.merge(cardId, best.fromCard, BigDecimal::add);
        }
    }

    private BigDecimal applyDiscount(BigDecimal value, int percent) {
        return value.multiply(BigDecimal.valueOf(100 - percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
