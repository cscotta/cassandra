# Bug 2040: StorageService.sampleKeyRange Uses Non-Existent Field Access

**Commit**: 1af5865edc · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java
**Category**: Wrong constant / default value

---

## Root Cause

In `sampleKeyRange`, the call to `getPrimaryRangesForEndpoint` passed `keyspace.name` (a direct field access) instead of `keyspace.getName()` (the proper getter method). After a refactor of the `Table` class during a merge, the public field no longer existed and the correct accessor was the method, causing a compilation failure.

### Before / After

```java
// BEFORE
for (Range<Token> range : getPrimaryRangesForEndpoint(keyspace.name, FBUtilities.getBroadcastAddress()))
```

```java
// AFTER
for (Range<Token> range : getPrimaryRangesForEndpoint(keyspace.getName(), FBUtilities.getBroadcastAddress()))
```

---

## How to Spot

When reviewing merge commits that touch classes with field-to-method renames, check all callsites that reference fields directly and verify whether the field was replaced by an accessor method in either branch being merged.

---

## Test

No test added.
