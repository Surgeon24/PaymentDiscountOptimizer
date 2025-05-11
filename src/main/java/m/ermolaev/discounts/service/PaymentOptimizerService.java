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
    private static class Option {
        String methodId;
        BigDecimal cost;
        BigDecimal fromLoyalty;
        BigDecimal fromCard;
    }

    public List<PaymentResult> optimize(List<Order> orders, List<PaymentMethod> methods) {
        List<List<PaymentResult>> allResults = new ArrayList<>();
        Random random = new Random();
        // if there are few orders - an attempt to sort through all options for a guaranteed best price
        if (orders.size() <= 8) {
            int maxPermutations = 1000;
            Set<List<Order>> triedPermutations = new HashSet<>();
            while (triedPermutations.size() < maxPermutations &&
                    triedPermutations.size() < factorial(orders.size())) {
                List<Order> permutation = new ArrayList<>(orders);
                Collections.shuffle(permutation, random);

                if (triedPermutations.add(permutation)) {
                    allResults.add(tryStrategy(permutation, methods));
                }
            }
        }
        // if there are many orders, check individual cases
        else {
            // 1. original order
            allResults.add(tryStrategy(orders, methods));

            // 2. sort by price descending
            orders.sort(Comparator.comparing(Order::getValue).reversed());
            allResults.add(tryStrategy(orders, methods));

            // 3. sort by price ascending
            Collections.reverse(orders);
            allResults.add(tryStrategy(orders, methods));

            // 4. 5 random orders to eliminate edge cases
            for (int i = 0; i < 5; i++) {
                Collections.shuffle(orders, random);
                allResults.add(tryStrategy(orders, methods));
            }
        }

        // find the best result
        return allResults.stream()
                .filter(Objects::nonNull)
                .min(Comparator.comparing(this::calculateTotalCost))
                .orElseThrow(() -> new RuntimeException("Failed to pay for all orders"));
    }

    private BigDecimal calculateTotalCost(List<PaymentResult> results) {
        return results.stream()
                .map(PaymentResult::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<PaymentResult> tryStrategy(List<Order> orders, List<PaymentMethod> methods) {
        Map<String, BigDecimal> spentPerMethod = new HashMap<>();
        Map<String, PaymentMethod> methodMap = new HashMap<>();

        // copy limits
        for (PaymentMethod method : methods) {
            methodMap.put(method.getId(), new PaymentMethod(method.getId(), method.getDiscount(), method.getLimit()));
            spentPerMethod.put(method.getId(), BigDecimal.ZERO);
        }
        try {
            // process each order
            for (Order order : orders) {
                if (!processOrder(order, methodMap, spentPerMethod)) {
                    return null;
                }
            }

            // final result
            List<PaymentResult> results = new ArrayList<>();
            for (Map.Entry<String, BigDecimal> entry : spentPerMethod.entrySet()) {
                if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                    results.add(new PaymentResult(entry.getKey(), entry.getValue().setScale(2, RoundingMode.HALF_UP)));
                }
            }

            return results;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean processOrder(Order order, Map<String, PaymentMethod> methodMap, Map<String, BigDecimal> spent) {
        BigDecimal orderValue = order.getValue();
        List<String> promos = order.getPromotions() != null ? order.getPromotions() : new ArrayList<>();

        List<Option> options = new ArrayList<>();
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
                BigDecimal maxPointsUsable = discounted.min(points.getLimit());

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

        if (options.isEmpty()) {
            return false;
        }
        // Choose best option
        options.sort(Comparator
                .comparing((Option o) -> o.cost)
                .thenComparing(o -> o.fromCard)
        );
        Option best = options.get(0);

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
        return true;
    }

    private BigDecimal applyDiscount(BigDecimal value, int percent) {
        return value.multiply(BigDecimal.valueOf(100 - percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private long factorial(int n) {
        if (n <= 1) return 1;
        return n * factorial(n - 1);
    }
}

