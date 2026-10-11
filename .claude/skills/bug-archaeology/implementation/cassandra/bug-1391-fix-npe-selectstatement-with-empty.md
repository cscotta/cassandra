# Bug 1391: Fix NPE in SelectStatement with empty IN values

**Commit**: 98cf63431a · **Ticket**: CASSANDRA-8419
**Subsystem**: cql3/restrictions/SingleColumnPrimaryKeyRestrictions.java
**Category**: Missing null / bounds check

---

## Root Cause

`SingleColumnPrimaryKeyRestrictions` iterated over IN restriction values to build composite clustering key bounds. When the IN list was empty (e.g. `WHERE ck IN ()`) `r.values(options)` returned an empty list and the method returned `null` to signal "no bounds". The callers of this method did not anticipate a `null` return and dereferenced it, causing a NullPointerException. The fix replaces both `null` returns with `Collections.emptyList()`, which callers handle correctly (an empty bounds list means no rows match, which is the semantically correct answer for an empty IN clause).

### Before / After

```java
// BEFORE — returns null for empty IN, callers NPE
List<ByteBuffer> values = r.values(options);
if (values.isEmpty())
    return null;
builder.addEachElementToAll(values);
```

```java
// AFTER — returns empty list, correctly short-circuits with zero results
List<ByteBuffer> values = r.values(options);
if (values.isEmpty())
    return Collections.emptyList();
builder.addEachElementToAll(values);
```

---

## How to Spot

Returning `null` to signal "no results" from a method that otherwise returns a `List` forces every caller to add a null check. Prefer returning an empty collection; it composes safely with all list operations and communicates the intent ("zero rows match") without a sentinel value. Code review should flag any method returning a nullable list when an empty list is semantically equivalent.

---

## Test

`SingleColumnRelationTest` and `MultiColumnRelationTest` — add cases for `SELECT ... WHERE ck IN ()` to verify no NPE and an empty result set is returned.
