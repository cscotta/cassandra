# Bug 1250: CLI Deprecation Warning Names Wrong Removal Version

**Commit**: f4ec535db4 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cli/CliClient.java
**Category**: Wrong constant / default value

---

## Root Cause

The CLI startup banner told users the tool would be removed in Cassandra 3.0, but the actual planned removal was Cassandra 2.2. The wrong version string was hardcoded in the warning message, giving operators misleading information about how long they had to migrate to cqlsh.

### Before / After

```java
// BEFORE
sessionState.out.println(
    "The CLI is deprecated and will be removed in Cassandra 3.0.  Consider migrating to cqlsh.");
```

```java
// AFTER
sessionState.out.println(
    "The CLI is deprecated and will be removed in Cassandra 2.2.  Consider migrating to cqlsh.");
```

---

## How to Spot

Version numbers embedded in user-facing strings are easy to leave stale during a rename or schedule change. Any deprecation, removal, or migration message that contains a version literal should be checked against the current roadmap whenever a release branch is cut or a removal date shifts.

---

## Test

No test added.
