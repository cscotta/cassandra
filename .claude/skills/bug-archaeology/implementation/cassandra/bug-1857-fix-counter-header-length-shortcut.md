# Bug 1857: Counter Header Length Shortcut Logic Checks Wrong Zero Value

**Commit**: eb577459df · **Ticket**: n/a
**Subsystem**: db/context (CounterContext)
**Category**: Wrong constant / default value

---

## Root Cause

`headerLength()` returns `HEADER_SIZE_LENGTH` (the 2-byte size prefix) as its minimum value when the header contains zero delta entries — it never returns `0`. Both `markDeltaToBeCleared` and `clearAllDelta` short-circuited with `if (headerLength == 0)`, a condition that can never be true, so those methods always did unnecessary work on already-clean contexts. Additionally, `validateContext` double-counted the header size by checking `headerLength < 0` separately when `headerLength` already includes the length prefix.

### Before / After

```java
// BEFORE
if (headerLength == 0)
    return context;
```

```java
// AFTER
if (headerLength == HEADER_SIZE_LENGTH)
    return context;
```

---

## How to Spot

When comparing a computed length against a sentinel "empty" value, verify what the minimum non-empty return value actually is rather than assuming it is `0`. A quick look at the return expression of the helper (`HEADER_SIZE_LENGTH + n * HEADER_ELT_LENGTH`) makes the floor value obvious.

---

## Test

No test added.
