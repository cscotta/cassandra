# Bug 2161: Duplicate field declaration and stale CassandraDaemon class left by bad merge

**Commit**: 989dd44c07 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/config/Config.java, src/java/org/apache/cassandra/thrift/CassandraDaemon.java
**Category**: Wrong constant / default value

---

## Root Cause

A bad branch merge left two artifacts: a duplicate field declaration `compaction_throughput_mb_per_sec` in `Config.java` (one `volatile`, one not), which is a compile error and would cause ambiguity about which copy is visible to concurrent readers; and a stale copy of `CassandraDaemon` in the `thrift` package that had already been superseded by the one in the `service` package. The fix removes the non-volatile duplicate field and deletes the entire stale daemon class.

### Before / After

```java
// BEFORE (Config.java) - duplicate field, one volatile and one not
public volatile Integer compaction_throughput_mb_per_sec = 16;
public Integer compaction_throughput_mb_per_sec = 16;  // duplicate from bad merge
```

```java
// AFTER - only the volatile declaration remains
public volatile Integer compaction_throughput_mb_per_sec = 16;
```

---

## How to Spot

After a merge, scan for duplicate field or method declarations, especially when one copy adds a modifier (`volatile`, `final`, `static`) that the other lacks. A compile-time error will catch the duplicate field case in Java, but the duplicate class in a different package is harder to notice without a full build.

---

## Test

No test added; the fix is purely structural cleanup of merge artifacts.
