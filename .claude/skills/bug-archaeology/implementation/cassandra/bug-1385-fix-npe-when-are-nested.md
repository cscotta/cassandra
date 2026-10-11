# Bug 1385: Fix NPE when writetime() or ttl() are nested inside a fn call

**Commit**: 3f3d0edbad · **Ticket**: CASSANDRA-8451
**Subsystem**: cql3/statements/Selection.java
**Category**: Missing null / bounds check

---

## Root Cause

`Selection.Factory.newSelectors()` determined whether to collect timestamps or TTLs by inspecting each top-level `Selector` with an `instanceof WritetimeOrTTLSelector` cast. When `writetime()` or `ttl()` was wrapped inside a user-defined function call, the top-level selector was a `FunctionSelector` rather than a `WritetimeOrTTLSelector`, so the flags were never set. At execution time the code assumed the required column metadata was present and dereferenced a null, causing an NPE.

The root cause is that introspection stopped at the outermost selector rather than recursing into function arguments; any selector subtree with a `WritetimeOrTTLSelector` leaf was invisible to the check.

### Before / After

```java
// BEFORE
if (selector instanceof WritetimeOrTTLSelector)
{
    collectTimestamps |= ((WritetimeOrTTLSelector)selector).isWritetime;
    collectTTLs |= !((WritetimeOrTTLSelector)selector).isWritetime;
}
```

```java
// AFTER — new interface methods propagate upward through FunctionSelector
collectTimestamps |= selector.usesTimestamps();
collectTTLs |= selector.usesTTLs();

// FunctionSelector delegates to its argument selectors recursively
public boolean usesTimestamps() {
    for (Selector s : argSelectors)
        if (s.usesTimestamps()) return true;
    return false;
}
```

---

## How to Spot

Whenever code checks `instanceof SomeLeafType` on items that may be wrapped in an arbitrary decorator or composite, it will silently miss the nested case. Prefer adding polymorphic query methods (`usesX()`) to an interface so that composite types can delegate to their children, ensuring the check propagates through the full tree.

---

## Test

No test added.
