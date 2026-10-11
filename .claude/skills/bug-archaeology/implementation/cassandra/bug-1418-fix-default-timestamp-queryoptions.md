# Bug 1418: Fix Default Timestamp in QueryOptions

**Commit**: e95e569c24 · **Ticket**: CASSANDRA-8246
**Subsystem**: cql3/QueryOptions
**Category**: Wrong constant / default value

---

## Root Cause

`QueryOptions.SpecificOptions` used `-1L` as the sentinel meaning "no timestamp provided; use server-generated time." However, `-1` is a valid microsecond epoch timestamp (one microsecond before the Unix epoch), not a safe out-of-band sentinel. When a query was executed without an explicit timestamp through a non-native path (e.g., internal calls), the timestamp field would be `-1`, which is a real value that compares less than any legitimate write timestamp. This caused two successive writes without explicit timestamps to compare as equal or inverted, so the reconciliation rules could pick the wrong cell value. The correct sentinel is `Long.MIN_VALUE`, which is the value the rest of the CQL stack already uses to mean "generate a fresh timestamp."

### Before / After

```java
// BEFORE — -1L is treated as "unset" but is a valid timestamp
private static final SpecificOptions DEFAULT = new SpecificOptions(-1, null, null, -1L);
```

```java
// AFTER — Long.MIN_VALUE is the correct out-of-band sentinel
private static final SpecificOptions DEFAULT = new SpecificOptions(-1, null, null, Long.MIN_VALUE);
```

---

## How to Spot

Whenever a numeric field uses a magic sentinel value (−1, 0, Integer.MAX_VALUE) to represent "absent," check whether that value lies within the domain of valid inputs. For timestamps in particular, the entire signed 64-bit range is potentially valid, so only `Long.MIN_VALUE` (or a boxed `null`) is safe as an absent marker. Look for sentinel comparisons in downstream consumers (`if (timestamp == -1)`) to confirm which value the rest of the system actually checks against.

---

## Test

`NonNativeTimestampTest.setServerTimestampForNonCqlNativeStatements` inserts two rows via internal (non-native) calls and verifies that each write receives a strictly increasing server-generated timestamp, which would fail if the sentinel were `-1L` and both writes shared the same wrong timestamp.
