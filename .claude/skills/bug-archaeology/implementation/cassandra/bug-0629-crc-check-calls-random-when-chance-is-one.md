# Bug 0629: CRC Check Always Calls ThreadLocalRandom Even When Check Chance Is 1.0

**Commit**: b885e9c054 · **Ticket**: CASSANDRA-14094
**Subsystem**: `src/java/org/apache/cassandra/io/compress/CompressedRandomAccessReader.java`, `org/apache/cassandra/streaming/compress/CompressedInputStream.java`
**Category**: Logic error in condition

---

## Root Cause

The CRC validation probability check used the expression `getCrcCheckChance() > ThreadLocalRandom.current().nextDouble()`. When `crc_check_chance` is set to `1.0` (the default: always check), the condition still unconditionally calls `ThreadLocalRandom.current().nextDouble()` to generate a random number before comparing. `ThreadLocalRandom.current().nextDouble()` returns a value in `[0.0, 1.0)`, so the comparison correctly triggers the check — but the random number generation is entirely wasted work that occurs on every compressed chunk read. The fix short-circuits the call: `getCrcCheckChance() >= 1d` is evaluated first; if true, the RNG is never invoked.

### Before / After

```java
// BEFORE – always calls ThreadLocalRandom even when chance == 1.0
if (getCrcCheckChance() > ThreadLocalRandom.current().nextDouble()) {
    // validate CRC
}
```

```java
// AFTER – skips RNG when check is always-on
if (getCrcCheckChance() >= 1d ||
    getCrcCheckChance() > ThreadLocalRandom.current().nextDouble()) {
    // validate CRC
}
```

---

## How to Spot

Any probability gate of the form `threshold > RNG.random()` should use short-circuit evaluation to skip the RNG call for the boundary values `0.0` (never) and `1.0` (always), which are by far the most common configurations. Reviewers should ask: "Does this call a side-effecting or expensive method even in the trivially true or trivially false case?" If yes, a guarding pre-check on the boundary values belongs before the RNG invocation.

---

## Test

No test added.
