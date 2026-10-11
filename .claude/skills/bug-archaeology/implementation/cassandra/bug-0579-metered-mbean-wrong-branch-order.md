# Bug 0579: Metered MBean Silently Unregistered Due to Wrong instanceof Order

**Commit**: 6a473c4dbe · **Ticket**: CASSANDRA-14628
**Subsystem**: src/java/org/apache/cassandra/metrics/CassandraMetricsRegistry.java
**Category**: Logic error in condition

---

## Root Cause

The `instanceof` chain in `CassandraMetricsRegistry` checked for `Meter` before `Timer`. Because `Timer` implements `Metered` (which `Meter` also implements), and the code used `new JmxMeter((Meter) metric, ...)` with a hard cast, any `Timer` instance would fall into the `Meter` branch first and cause a `ClassCastException` that was swallowed by `catch (Exception ignored)`, silently leaving the Timer unregistered as an MBean. The fix reorders the branches so `Timer` is checked first and adds an `instanceof Metered` guard for the meter branch, accepting the broader `Metered` interface.

### Before / After

```java
// BEFORE
} else if (metric instanceof Meter) {
    mbean = new JmxMeter((Meter) metric, name, TimeUnit.SECONDS);
} else if (metric instanceof Timer) {
    mbean = new JmxTimer((Timer) metric, name, TimeUnit.SECONDS, TimeUnit.MICROSECONDS);
}
```

```java
// AFTER
else if (metric instanceof Timer)
    mbean = new JmxTimer((Timer) metric, name, TimeUnit.SECONDS, TimeUnit.MICROSECONDS);
else if (metric instanceof Metered)
    mbean = new JmxMeter((Metered) metric, name, TimeUnit.SECONDS);
```

---

## How to Spot

In `instanceof` dispatch chains involving a type hierarchy, always check the most-derived types first. When a caught exception block is empty (`ignored`), failures are completely invisible — any class-cast that happens inside will prevent registration without any diagnostic, so type-hierarchy ordering bugs can persist silently for a long time.

---

## Test

No test added.
