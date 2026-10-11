# Bug 1892: Fix build — diamond operator incompatibility in index searchers

**Commit**: 102c3c123e · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/index
**Category**: Wrong constant / default value

---

## Root Cause

`CompositesSearcher` and `KeysSearcher` both used the Java 7 diamond operator (`new HashMap<>()`) in a context that was compiled against a Java 6 source/target level. The diamond operator for anonymous or non-inferrable contexts was unavailable in Java 6 generics inference, causing a compilation failure. The fix explicitly types the generic parameters, restoring Java 6 compatibility.

### Before / After

```java
// BEFORE
Map<SecondaryIndex, Integer> candidates = new HashMap<>();
```

```java
// AFTER
Map<SecondaryIndex, Integer> candidates = new HashMap<SecondaryIndex, Integer>();
```

---

## How to Spot

When a project has a declared minimum Java version, any use of language features from a newer version (diamond operator inference, try-with-resources on older targets) will break the build. CI should enforce `-source`/`-target` compiler flags matching the declared minimum.

---

## Test

No test added.
