# Bug 0286: Wrong Default Value for compaction_throughput_mb_per_sec in Config

**Commit**: e5e13c02cc · **Ticket**: CASSANDRA-17790
**Subsystem**: config/Config
**Category**: Wrong constant / default value

---

## Root Cause

`Config.compaction_throughput_mb_per_sec` was initialised to `16` in the Java class, but `cassandra.yaml` specified a default of `64`. Any node that omitted the setting from its `cassandra.yaml` (relying on the programmatic default) would silently use 16 MiB/s instead of the documented 64 MiB/s, causing significantly slower compaction than operators expected. The two sources of truth diverged at some point and were never reconciled.

### Before / After

```java
// BEFORE
public volatile int compaction_throughput_mb_per_sec = 16;
```

```java
// AFTER
public volatile int compaction_throughput_mb_per_sec = 64;
```

---

## How to Spot

Any configuration property that has a default value both in a YAML template and in a Java field is a candidate for this class of divergence. A linting rule or a test that parses the default YAML and compares each value against the Java field default would catch these mismatches automatically.

---

## Test

No new test added; the fix is a single constant change, and `NEWS.txt` was updated to warn operators who relied on the old implicit default.
