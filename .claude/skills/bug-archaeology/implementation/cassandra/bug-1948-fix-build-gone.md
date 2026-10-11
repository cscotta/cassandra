# Bug 1948: StandaloneSplitter build failure after log4j removal

**Commit**: 3f5322f02a · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/tools/StandaloneSplitter.java
**Category**: State not cleaned up

---

## Root Cause

`StandaloneSplitter` contained a static initializer that called `CassandraDaemon.initLog4j()` to configure logging. When log4j was removed from Cassandra's dependencies as part of the 2.0 migration, this call became a compilation error because the method no longer existed. The fix removes the static initializer block and the now-unused `CassandraDaemon` import, allowing the tool to build and run without the removed log4j initialization hook.

### Before / After

```java
// BEFORE — static initializer references removed API
static
{
    CassandraDaemon.initLog4j();
}
```

```java
// AFTER — static initializer removed entirely
// (no replacement needed; logging is initialized elsewhere)
```

---

## How to Spot

Standalone tool classes that copied boilerplate initialization blocks from the daemon class accumulate dead references when the daemon's initialization surface changes. When a dependency is removed, search for all call sites of its initialization helpers (not just the dependency import itself) to find stranded initializers in non-daemon tools.

---

## Test

No test added.
