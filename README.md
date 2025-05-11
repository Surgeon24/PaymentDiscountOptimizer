# Mikhail_Ermolaev_Java_Krakow

## Payment Promotion Optimizer

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
