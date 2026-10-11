# Bug 013: Wrong Default Max Interval Calculation in FailureDetector

**Commit**: f8f456f0f674090652e64ac4f5aa276bf81f621d · **Ticket**: CASSANDRA-21025
**Subsystem**: Gossip / FailureDetector
**Category**: Wrong constant / default value

---

## Root Cause

`ArrivalWindow.getMaxInterval()` reads the system property `FD_MAX_INTERVAL_MS` using `INITIAL_VALUE_NANOS` (2,000,000,000 ns = 2 s expressed in nanoseconds) as the *default* value passed to `getLong()`. The property is documented and intended to be supplied in **milliseconds**, so the default fallback should also be expressed in milliseconds (2,000 ms). By passing the raw nanosecond constant as the default, any deployment that did not explicitly set the property received a default of 2,000,000,000 ms (roughly 23 days) instead of 2 seconds, making the failure detector effectively non-functional for endpoints that had never sent a heartbeat. The fix converts `INITIAL_VALUE_NANOS` to milliseconds before using it as the default argument.

### Before / After

```java
// BEFORE — default supplied in nanoseconds to a millis-unit property
private static long getMaxInterval()
{
    long newValue = FD_MAX_INTERVAL_MS.getLong(FailureDetector.INITIAL_VALUE_NANOS);
    if (newValue != FailureDetector.INITIAL_VALUE_NANOS)
        logger.info("Overriding {} from {}ms to {}ms", ...);
    return TimeUnit.NANOSECONDS.convert(newValue, TimeUnit.MILLISECONDS);
}

// AFTER — default converted to millis before lookup
static long getMaxInterval()
{
    long newValue = FD_MAX_INTERVAL_MS.getLong(
        TimeUnit.NANOSECONDS.toMillis(FailureDetector.INITIAL_VALUE_NANOS));
    if (newValue != FailureDetector.INITIAL_VALUE_NANOS)
        logger.info("Overriding {} from {}ms to {}ms", ...);
    return TimeUnit.NANOSECONDS.convert(newValue, TimeUnit.MILLISECONDS);
}
```

---

## How to Spot

Whenever a constant is used as a fallback default for a configuration property that operates in a *different* unit from the constant's definition, a unit-mismatch bug is likely. Reviewers should verify that the units of the default argument match the units expected by the property (here "MS" in the property name is the critical clue). A simple sanity check is to ask: "If the property is absent, what value does this expression produce, and is that value reasonable in the context where it is consumed?"

---

## Test

`testMaxIntervalCalculation` clears the `FD_MAX_INTERVAL_MS` property, asserts that `calculateMaxInterval()` returns `INITIAL_VALUE_NANOS` (2 seconds in nanoseconds), then sets an explicit override and asserts the correctly converted nanosecond value is returned.
