# Bug 2186: Wrong field name after merge leaves connection pool unreferenced

**Commit**: f0114cfe7e · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/net/MessagingService.java
**Category**: Logic error in condition

---

## Root Cause

A merge from CASSANDRA-4840 renamed the `connectionManagers_` field to `connectionManagers` in `MessagingService`, but the `destroyConnectionPool` method was not updated consistently. The method looked up the pool using the old field name `connectionManagers_` and removed it from `connectionManagers_` as well, meaning it operated on a stale/wrong map and the connection pool was never actually removed from the live map. The fix replaces all three references with the correct field name `connectionManagers`.

### Before / After

```java
// BEFORE
OutboundTcpConnectionPool cp = connectionManagers_.get(to);
// ...
connectionManagers_.remove(to);
```

```java
// AFTER
OutboundTcpConnectionPool cp = connectionManagers.get(to);
// ...
connectionManagers.remove(to);
```

---

## How to Spot

After a field rename refactor, search for all references to the old name. Merge commits that rename fields are especially risky if different methods are in separate merge branches and the rename is only partially applied.

---

## Test

No test added.
