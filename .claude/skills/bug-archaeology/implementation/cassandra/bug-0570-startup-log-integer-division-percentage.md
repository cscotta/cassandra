# Bug 0570: Startup Connectivity Log Shows Integer-Division Percentage

**Commit**: c3ef43d45e · **Ticket**: CASSANDRA-14836
**Subsystem**: `src/java/org/apache/cassandra/net/StartupClusterConnectivityChecker.java`
**Category**: Wrong constant / default value

---

## Root Cause

The connectivity check log statement computed a connectivity percentage using integer division (`connected / peers.size() * 100.0`). Because both operands are `int`, the division truncates to zero whenever fewer peers than total are connected, and the subsequent multiplication by `100.0` produces only multiples of 100 (i.e., 0 or 100). The fix casts the divisor to `float` before division and formats the result to two decimal places, producing accurate fractional percentages.

### Before / After

```java
// BEFORE
connected / (peers.size()) * 100.0
```

```java
// AFTER
String.format("%.2f", (connected / (float)peers.size()) * 100)
```

---

## How to Spot

Any arithmetic expression of the form `intA / intB * doubleC` performs integer division first; the cast to `double` only happens after the truncation. Review log/message formatting expressions that compute percentages or ratios: if the numerator and denominator are both integral types, force a floating-point cast on at least one operand before the division, or use `100.0 * intA / intB`.

---

## Test

No test added.
