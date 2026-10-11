# Bug 2012: ReadCallback.get() Uses Microseconds Instead of Milliseconds for Timeout

**Commit**: b2ba251ac6 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/service/ReadCallback.java
**Category**: Wrong constant / default value

---

## Root Cause

`ReadCallback.get()` calls `await(command.getTimeout(), TimeUnit.MICROSECONDS)` to block until read responses arrive or a timeout fires. `command.getTimeout()` returns a value in milliseconds, so passing `TimeUnit.MICROSECONDS` causes the latch to wait 1000 times less than intended. This means read operations would time out approximately 1000x too early under any non-trivial load, producing spurious `ReadTimeoutException` errors even when responses would have arrived within the configured timeout window.

### Before / After

```java
// BEFORE
if (!await(command.getTimeout(), TimeUnit.MICROSECONDS))
```

```java
// AFTER
if (!await(command.getTimeout(), TimeUnit.MILLISECONDS))
```

---

## How to Spot

Whenever a timeout value is passed alongside an explicit `TimeUnit`, verify that the unit matches the documented or actual unit of the value source. Mismatched units in timeout calls are especially dangerous because the code compiles and runs without error — the symptom is only observable as unexpected timeout behavior under load.

---

## Test

No test added.
