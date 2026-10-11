# Bug 1365: Avoid expensive string op when trace logging is not enabled

**Commit**: bd3c47ca73 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/index/composites/CompositesSearcher.java
**Category**: Logic error in condition

---

## Root Cause

Inside the hot secondary-index search path of `CompositesSearcher`, a call to `indexComparator.getString(column.name())` was passed directly as a format argument to `logger.trace(...)`. SLF4J's parameterised logging defers string formatting until the message is actually appended, but `getString()` is invoked eagerly as a method argument before the logger level check takes place. On production clusters with TRACE disabled — the overwhelming common case — every index hit triggered an unnecessary, potentially expensive byte-to-string conversion on every candidate column, wasting CPU on secondary-index reads.

### Before / After

```java
// BEFORE — getString() called unconditionally on every index hit
logger.trace("Adding index hit to current row for {}",
             indexComparator.getString(column.name()));
```

```java
// AFTER — getString() only called when trace is actually enabled
if (logger.isTraceEnabled())
    logger.trace("Adding index hit to current row for {}",
                 indexComparator.getString(column.name()));
```

---

## How to Spot

SLF4J's `{}` placeholder defers *string concatenation*, but it does not defer evaluation of the *arguments themselves*. Any argument that involves a non-trivial method call (type serialization, collection iteration, comparator-driven string conversion) must be guarded with `logger.isTraceEnabled()` / `logger.isDebugEnabled()`. During performance-sensitive code review, look for `logger.trace(...)` or `logger.debug(...)` calls whose arguments include method invocations rather than simple field reads.

---

## Test

No test added.
