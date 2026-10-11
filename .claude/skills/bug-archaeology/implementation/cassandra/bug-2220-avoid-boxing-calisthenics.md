# Bug 2220: Unnecessary Boolean Boxing in ConfigHelper Hadoop Configuration Read

**Commit**: f75026a17f · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/hadoop/ConfigHelper.java
**Category**: Logic error in condition

---

## Root Cause

`ConfigHelper.getInputIsWide` called `Boolean.valueOf(conf.get(INPUT_WIDEROWS_CONFIG))`, which boxes the result to a `Boolean` object before auto-unboxing it to a primitive `boolean`. If the configuration key is absent, `conf.get(...)` returns `null`, and `Boolean.valueOf(null)` returns `Boolean.FALSE` via `parseBoolean("null")`, but the intermediate object allocation is wasteful and the intent is obscured. Using `Boolean.parseBoolean(...)` directly returns a primitive `boolean` without the boxing step, making the intent clear and avoiding unnecessary object allocation.

### Before / After

```java
// BEFORE
return Boolean.valueOf(conf.get(INPUT_WIDEROWS_CONFIG));
```

```java
// AFTER
return Boolean.parseBoolean(conf.get(INPUT_WIDEROWS_CONFIG));
```

---

## How to Spot

When returning a `boolean` primitive, prefer `Boolean.parseBoolean(str)` over `Boolean.valueOf(str)` to avoid creating a wrapper object that is immediately unboxed. Review any location that wraps a string in `Boolean.valueOf` and immediately returns or unboxes the result.

---

## Test

No test added.
