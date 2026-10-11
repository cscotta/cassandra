# Bug 1598: Fix Infinite Loop on Exception While Streaming

**Commit**: 58bb974a33 · **Ticket**: CASSANDRA-7330
**Subsystem**: src/java/org/apache/cassandra/streaming/StreamReader.java
**Category**: Logic error in condition

---

## Root Cause

`StreamReader.drain()` used a `while` loop to skip remaining bytes from a stream after reading, subtracting the result of `InputStream.skip()` each iteration. However, `InputStream.skip()` is permitted by the Java contract to return `-1` when the stream is inaccessible or closed. When that happened, subtracting `-1` caused `toSkip` to *increase* by 1 each iteration instead of decreasing, making the loop condition (`toSkip > 0`) permanently true and spinning forever.

### Before / After

```java
// BEFORE: -1 return from skip() increments toSkip, looping forever
toSkip = toSkip - dis.skip(toSkip);
while (toSkip > 0)
    toSkip = toSkip - dis.skip(toSkip);
```

```java
// AFTER: -1 return is detected and treated as unrecoverable; loop exits cleanly
long skipped = dis.skip(toSkip);
if (skipped == -1)
    return;

toSkip = toSkip - skipped;
while (toSkip > 0)
{
    skipped = dis.skip(toSkip);
    if (skipped == -1)
        break;
    toSkip = toSkip - skipped;
}
```

---

## How to Spot

When using `InputStream.skip()` in a drain/flush loop, always check for a `-1` return value before subtracting it from the remaining count. A loop convergence review—"does the loop variable always move closer to termination?"—would catch this: adding 1 when -1 is returned violates the convergence invariant.

---

## Test

No test added.
