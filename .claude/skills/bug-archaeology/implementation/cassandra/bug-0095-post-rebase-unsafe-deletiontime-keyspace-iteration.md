# Bug 0095: Multiple Post-Rebase Fixes: Unsafe DeletionTime, Wrong nowInSec Type, and Missing Accord Keyspace in system()

**Commit**: 68135aaf29 · **Ticket**: n/a
**Subsystem**: db — DeletionTime, Keyspace, SinglePartitionReadCommand; service/accord — AccordTopologyUtils
**Category**: Wrong constant / default value

---

## Root Cause

After rebasing the cep-15-accord branch onto cep-21-tcm, several call sites used APIs that were either removed, renamed, or narrowed in incompatible ways. The most impactful fixes are: (1) Multiple callers passed `nowInSec` as an `int` to `DeletionTime.buildUnsafeWithUnsignedInteger` — the "unsafe" variant deliberately skips overflow and corruption validation; switching to `DeletionTime.build` restores the validation gates. (2) `Keyspace.system()` iterated `SchemaConstants.LOCAL_SYSTEM_KEYSPACE_NAMES`, a static constant that predates TCM and does not include the Accord keyspace; the fix switches to `Schema.instance.localKeyspaces().names()` which is dynamically populated and includes all local keyspaces including `accord`. (3) `SinglePartitionReadCommand.withNowInSec` accepted `int` but the Accord path now supplies a `long`; the signature was widened to `long`. (4) `AccordTopologyUtils.createTopology()` threw `UnsupportedOperationException` with a TODO comment after the rebase; it was reimplemented using `ClusterMetadata` from TCM.

### Before / After

```java
// BEFORE — bypasses validation
partitionDeletion = DeletionTime.buildUnsafeWithUnsignedInteger(timestamp, localDeletionTime);
```

```java
// AFTER — validates and rejects corrupt values
partitionDeletion = DeletionTime.build(timestamp, localDeletionTime);
```

```java
// BEFORE — misses Accord and other TCM-managed local keyspaces
return Iterables.transform(SchemaConstants.LOCAL_SYSTEM_KEYSPACE_NAMES, Keyspace::open);
```

```java
// AFTER — dynamically includes all local keyspaces
return Iterables.transform(Schema.instance.localKeyspaces().names(), Keyspace::open);
```

---

## How to Spot

After any large rebase, search for callers of APIs with "Unsafe" or "internal" in their names — these are often temporary workarounds that become incorrect when surrounding invariants change. Also, any static constant that enumerates keyspace or table names (instead of querying a live schema registry) is fragile: when new system keyspaces are added (as happens with TCM/Accord), the constant must be updated manually or replaced with a dynamic lookup.

---

## Test

No new tests added; the fixes are mechanical corrections to post-rebase compilation and correctness issues.
