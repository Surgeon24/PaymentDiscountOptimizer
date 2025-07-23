## Payment Promotion Optimizer
### Description
This project implements a payment optimization algorithm for an online supermarket that supports both traditional payment methods (e.g., credit cards, bank transfers) and loyalty points.

### 💳 Payment Rules <br>
Customers can pay for each order using:

* A single traditional method (e.g., one card),
* Loyalty points (fully),
* A combination of loyalty points and one traditional method.

### 🎁 Discount Rules <br>
Discounts are applied based on the payment method:

* Bank Card Discounts
If an order is fully paid with a card from a specific bank (with which we have a partnership), a percentage discount is applied. These discounts are assigned per order and are only valid for full payments using a specific method.
Partial payments using such cards do not qualify for the discount.

* Loyalty Points Discounts

If at least 10% of the order (before discount) is paid with loyalty points, an additional 10% discount is applied to the entire order.

If the entire order is paid with loyalty points, a special discount defined for the "LOYALTY_POINTS" method is applied instead of the 10% partial-points bonus.

Loyalty point discounts are global, not assigned per order.

### 📌 Constraints Summary
* Each order has a predefined list of eligible promotions (based on payment methods).
* Discounts for full card payments are applied only if the order is paid 100% with that card.
* The loyalty discount for partial payments requires using points for at least 10% of the order.
* Loyalty and card discounts are mutually exclusive — only one can be applied.
* All orders must be fully paid using available methods and limits in the customer’s wallet.

### 🎯 Goal <br>
Design an algorithm that:

* Chooses the optimal payment method(s) for each order from the customer's wallet,

* Maximizes the total discount received,

* Ensures all orders are fully paid,

* Minimizes the use of cards, preferring loyalty points where it does not reduce the overall discount.

## Implementation

Java 21 console application that selects the optimal payment method for each order to maximize the total discount. Supports loyalty points, bank discounts, and complex decision logic.

## Features
For ≤10 orders:
Generates all unique permutations (up to 1000)
Guaranteed to find an optimal solution if it exists
Limits the maximum number of attempts for safety

For >10 orders:
Uses uses 7 different approaches
Provides a good balance between quality and performance

## How to Build

Build the fat JAR using Gradle:

```bash
./gradlew clean shadowJar
```
This will produce: build/libs/discounts-0.1.0-all.jar

## How to Run

```bash
java -jar discounts-0.1.0-all.jar /absolute/path/to/orders.json /absolute/path/to/paymentmethods.json
```

Output:
```
PUNKTY 100.00
mZysk 165.00
BosBankrut 190.00
```
