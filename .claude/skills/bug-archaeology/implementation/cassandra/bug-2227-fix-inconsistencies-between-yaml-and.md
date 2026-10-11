# Bug 2227: Wrong Default Values for max_hint_window_in_ms and in_memory_compaction_limit_in_mb

**Commit**: d170a7a99d · **Ticket**: CASSANDRA-4754
**Subsystem**: src/java/org/apache/cassandra/config/Config.java
**Category**: Wrong constant / default value

---

## Root Cause

Two configuration defaults in `Config.java` differed from the values documented in `cassandra.yaml`. `max_hint_window_in_ms` was set to `Integer.MAX_VALUE` (over 24 days) instead of the documented one-hour value (`3600 * 1000`), meaning hints would be stored indefinitely by default. `in_memory_compaction_limit_in_mb` was set to 256 MB instead of the documented 64 MB, consuming four times more heap during compaction if the yaml value was absent.

### Before / After

```java
// BEFORE
public Integer max_hint_window_in_ms = Integer.MAX_VALUE;
public Integer in_memory_compaction_limit_in_mb = 256;
```

```java
// AFTER
public Integer max_hint_window_in_ms = 3600 * 1000; // one hour
public Integer in_memory_compaction_limit_in_mb = 64;
```

---

## How to Spot

When configuration defaults are duplicated between a YAML template and a Java `Config` class, keep them in sync. Any time a `Config` field initializer uses a round number or sentinel like `Integer.MAX_VALUE`, verify it matches the default stated in the yaml and documentation.

---

## Test

No test added.
