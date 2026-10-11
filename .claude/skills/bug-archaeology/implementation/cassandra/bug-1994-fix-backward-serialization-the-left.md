# Bug 1994: Gossip LEFT State Serialized With Swapped Arguments

**Commit**: 54266ea705 · **Ticket**: n/a (CASSANDRA-5696 via CHANGES.txt)
**Subsystem**: src/java/org/apache/cassandra/gms/VersionedValue.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

The `VersionedValue.left()` factory method builds a versioned gossip string encoding a node's LEFT status. It calls `versionString(STATUS_LEFT, expireTime, tokens)` but the arguments were passed in the wrong order: `expireTime` was first and `tokens` second. Consumers that parsed the string expecting tokens in position one and expireTime in position two would misinterpret the value, causing incorrect decommission handling in mixed-version clusters.

### Before / After

```java
// BEFORE
return new VersionedValue(versionString(VersionedValue.STATUS_LEFT,
        Long.toString(expireTime),
        makeTokenString(tokens)));
```

```java
// AFTER
return new VersionedValue(versionString(VersionedValue.STATUS_LEFT,
        makeTokenString(tokens),
        Long.toString(expireTime)));
```

---

## How to Spot

Calls to variadic or positional-string-format methods with multiple arguments of compatible types (e.g., two `String` values) are invisible to the compiler. When the method's callers rely on positional ordering to parse back the result, a swap is a silent semantic error. Review every call site that constructs a versioned/composite string to confirm argument order matches the parsing order.

---

## Test

No test added.
