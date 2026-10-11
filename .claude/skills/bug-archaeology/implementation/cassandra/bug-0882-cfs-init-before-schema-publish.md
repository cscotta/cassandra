# Bug 0882: Race condition initializing CFS before schema is visible to other threads

**Commit**: 2e4763691c · **Ticket**: CASSANDRA-12083
**Subsystem**: Schema / Keyspace management
**Category**: Race condition / missing synchronization

---

## Root Cause
In `Schema.addTable()`, the code first updated the in-memory keyspace metadata map (making the new `CFMetaData` visible to other threads via `update()`), and only then called `Keyspace.initCf()` to actually initialize the `ColumnFamilyStore`. Any thread that observed the updated keyspace metadata between those two steps could attempt to use the CFS before it existed, triggering a NPE or IllegalStateException (as seen in CASSANDRA-10761). The fix reorders the operations: `initCf` is called before the metadata is published via `update()`, eliminating the window.

### Before / After
```java
// BEFORE — metadata visible before CFS initialized
Keyspace.open(cfm.ksName);
update(cfm.ksName, ks -> ks.withSwapped(ks.tables.with(cfm)));  // now visible!
load(cfm);
Keyspace.open(cfm.ksName).initCf(cfm, true);                    // too late

// AFTER — CFS initialized before metadata is published
Keyspace.open(cfm.ksName).initCf(cfm, true);                    // init first
update(cfm.ksName, ks -> ks.withSwapped(ks.tables.with(cfm)));  // then publish
load(cfm);
```

---

## How to Spot
When reviewing schema-change code, verify that initialization of mutable resources (CFS, index managers, etc.) always precedes the point at which those resources become visible to other threads through shared data structures. If a `update()`/`put()` call that publishes a new object appears before the object is fully constructed, that is this bug pattern.

---

## Test
No new test was added in this commit.
