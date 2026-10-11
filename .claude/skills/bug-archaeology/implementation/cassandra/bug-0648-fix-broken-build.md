# Bug 0648: Removed isForThrift Parameter Breaks DataResolver Build

**Commit**: f1b7d6e8ea · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/service/DataResolver.java
**Category**: Wrong constant / default value

---

## Root Cause

The `SinglePartitionReadCommand.create()` factory method was refactored to drop its `isForThrift` first parameter, but one call-site inside `DataResolver`'s short-read-protection retry logic was not updated. This left a stale two-argument call that no longer matched any overload, causing a compilation failure.

### Before / After

```java
// BEFORE
return SinglePartitionReadCommand.create(command.isForThrift(),
                                         command.metadata(),
                                         command.nowInSec(),
                                         ...);
```

```java
// AFTER
return SinglePartitionReadCommand.create(command.metadata(),
                                         command.nowInSec(),
                                         ...);
```

---

## How to Spot

When a factory or constructor signature changes (especially removing a parameter), search for every call-site in the codebase before committing — a single missed call-site turns a clean refactor into a broken build. IDE cross-reference tools and `grep` for the old signature are the minimum check.

---

## Test

No test added.
