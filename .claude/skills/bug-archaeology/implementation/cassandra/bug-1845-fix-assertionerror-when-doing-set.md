# Bug 1845: Fix AssertionError When Doing Set Element Deletion

**Commit**: 4c08800b4c · **Ticket**: CASSANDRA-6341
**Subsystem**: src/java/org/apache/cassandra/cql3 (CQL operation dispatch)
**Category**: Wrong constant / default value

---

## Root Cause

In `Operation.java`, the case that builds a deletion operation for a `SET` collection element incorrectly instantiated `Lists.Discarder` instead of `Sets.Discarder`. `Lists.Discarder` operates on list indices and asserts that the element value is an integer index, so passing a set element value triggered an `AssertionError` at runtime. The fix is a one-word change substituting the correct discarder class.

### Before / After

```java
// BEFORE (Operation.java)
case SET:
    Term elt = element.prepare(Sets.valueSpecOf(receiver));
    return new Lists.Discarder(id, elt);   // wrong class
```

```java
// AFTER (Operation.java)
case SET:
    Term elt = element.prepare(Sets.valueSpecOf(receiver));
    return new Sets.Discarder(id, elt);    // correct class
```

---

## How to Spot

In switch/case dispatch that constructs type-specific objects, verify that every case uses a class consistent with the case label — a `SET` case creating a `Lists.*` object is an immediate red flag. Code-review tools or simple name-matching lints would catch this.

---

## Test

No test added.
