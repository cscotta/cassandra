# Bug 0062: RetryStrategy Min-Bound Has No Cap, Causing Unbounded Backoff in CMS Retry

**Commit**: 265511f6de · **Ticket**: CASSANDRA-20529
**Subsystem**: service/RetryStrategy, config
**Category**: Wrong constant / default value

---

## Root Cause

`RetryStrategy` supports a formula of the form `min_expr ... max_expr` to express a random backoff range. The `min_expr` (lower bound of the random window) could grow without limit as attempts increased (e.g. `50ms*attempts`) because there was no syntax or logic to cap it. Without a cap, at high attempt counts `min` could exceed `max`, causing the backoff to collapse to a fixed value — or worse, the two bounds could diverge so far that the retry delay became enormous. The default `cms_retry_delay` string also used `0 <= ...` (an absolute floor of zero) rather than a properly growing minimum, which made early CMS retries happen almost immediately and caused test flakiness (`CQLSSTableWriterConcurrencyTest`). The fix adds a `<= maxmin` clause to the grammar so the min expression can be capped (e.g. `50ms*attempts <= 500ms`), and updates the default config string to use a sensible growing minimum with a cap.

### Before / After

```java
// BEFORE – no cap on min expression; default zero floor
public String cms_retry_delay = "0 <= 50ms*1*attempts <= 1s,retries=10";
```

```java
// AFTER – growing min with explicit cap; sensible default
public String cms_retry_delay = "50ms*attempts <= 500ms ... 100ms*attempts <= 1s,retries=10";
```

```java
// BEFORE – RetryStrategy.compute: min grows unbounded
long min = this.min.getMicros(attempt);
long max = this.max.getMicros(attempt);
result = min >= max ? min : waitRandomizer.wait(min, max, attempt);
```

```java
// AFTER – min is capped at maxMinMicros
long min = this.min.getMicros(attempt);
if (min > maxMinMicros)
    min = maxMinMicros;
long max = this.max.getMicros(attempt);
result = min >= max ? min : waitRandomizer.wait(min, max, attempt);
```

---

## How to Spot

When a backoff formula's lower bound grows with attempt count but has no ceiling, it can surpass the upper bound or grow to an unexpectedly large value. Review retry-strategy DSLs for independent min and max expressions and ensure the min expression is capped below the max expression. Also check that default configuration strings for retry delays actually produce retries with sane low-attempt latencies.

---

## Test

No new unit test added; the fix was validated by `CQLSSTableWriterConcurrencyTest` (the flaky test that motivated the change) now passing reliably.
