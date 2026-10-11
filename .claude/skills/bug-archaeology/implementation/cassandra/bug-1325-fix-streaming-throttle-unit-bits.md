# Bug 1325: Streaming Throttle Unit Wrong (Bits vs. Bytes)

**Commit**: da9f030c29 · **Ticket**: CASSANDRA-8852
**Subsystem**: streaming/StreamManager
**Category**: Wrong constant / default value

---

## Root Cause

The `StreamRateLimiter` constant `ONE_MEGA_BIT` was computed as `1024 * 1024 * 8`, which is the number of bits in a megabyte — 64 times larger than the number of bytes in a megabit. The configuration value `getStreamThroughputOutboundMegabitsPerSec()` is in megabits per second, so converting it to a byte-based rate limit requires dividing by 8, not multiplying by 8. As a result, the actual enforced rate limit was 64× higher than the operator configured, making throttling essentially ineffective.

### Before / After

```java
// BEFORE
private static final double ONE_MEGA_BIT = 1024 * 1024 * 8;
// megabits/s * (1024*1024*8 bytes/megabit) = bytes/s  ← wrong: multiplied instead of divided
double throughput = DatabaseDescriptor.getStreamThroughputOutboundMegabitsPerSec() * ONE_MEGA_BIT;
```

```java
// AFTER
private static final double BYTES_PER_MEGABIT = 1024 * 1024 / 8;
// megabits/s * (1024*1024/8 bytes/megabit) = bytes/s  ← correct
double throughput = DatabaseDescriptor.getStreamThroughputOutboundMegabitsPerSec() * BYTES_PER_MEGABIT;
```

---

## How to Spot

Unit-conversion constants are highly error-prone. Whenever a constant name contains "bit" or "byte" alongside a multiplier, verify the conversion direction: bits-to-bytes divides by 8, bytes-to-bits multiplies by 8. A useful check is to dimensional-analyze the formula: `[Mb/s] * [bytes/Mb]` should produce `[bytes/s]`, so the conversion factor must be bytes-per-megabit (`1024*1024/8`), not bits-per-megabyte. Renaming the constant to `BYTES_PER_MEGABIT` makes the intended unit explicit and the error immediately visible.

---

## Test

No test added.
