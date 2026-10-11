# Bug 0590: Log Arguments Evaluated Eagerly When Log Level Is Disabled

**Commit**: 4d8fc5b050 · **Ticket**: CASSANDRA-14488
**Subsystem**: Multiple — `db/`, `io/sstable/`, `streaming/`, `repair/`, `gms/`, `net/`, `utils/`
**Category**: Logic error in condition

---

## Root Cause

Across roughly 20 call sites, log statements at `trace`, `debug`, or `info` level were called unconditionally without first checking whether the corresponding log level was enabled. SLF4J's parameterised `{}` placeholders defer string formatting, but argument expressions (such as `Iterables.toString(endpoints)`, `Arrays.asList(replicas)`, `FBUtilities.prettyPrintMemory(size)`, or `String.format(...)`) are still evaluated eagerly before the call, consuming CPU and heap even when the log output is discarded. In hot paths such as compaction, consistency-level checking, and streaming, this overhead was non-trivial. The fix wraps each call site with an `if (logger.isXxxEnabled())` guard.

### Before / After

```java
// BEFORE — argument expressions evaluated unconditionally
logger.trace("Live nodes {} do not satisfy ConsistencyLevel ({} required)",
             Iterables.toString(liveEndpoints), blockFor);

logger.debug(String.format("Compacted (%s) %d sstables ...", taskId, ...));
```

```java
// AFTER — argument expressions skipped when level is disabled
if (logger.isTraceEnabled())
    logger.trace("Live nodes {} do not satisfy ConsistencyLevel ({} required)",
                 Iterables.toString(liveEndpoints), blockFor);

if (logger.isDebugEnabled())
    logger.debug(String.format("Compacted (%s) %d sstables ...", taskId, ...));
```

---

## How to Spot

Any log call whose argument list contains a method call that builds a collection string, formats memory sizes, or constructs a `String.format` result should be checked for an enclosing `isXxxEnabled()` guard. Static analysis rules (e.g., PMD's `GuardLogStatement`) catch this class of issue automatically.

---

## Test

No test added.
