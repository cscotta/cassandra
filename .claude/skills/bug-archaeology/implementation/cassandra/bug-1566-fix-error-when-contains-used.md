# Bug 1566: Fix Error When CONTAINS Is Used with a Bind Marker

**Commit**: 0c6fad4a8a · **Ticket**: CASSANDRA-7502
**Subsystem**: cql3 / statements / SelectStatement
**Category**: Missing null / bounds check

---

## Root Cause

When a `CONTAINS` (or `CONTAINS KEY`) restriction was merged with an existing restriction of the same type during statement preparation, the code called `newRel.getValue().prepare()` to build the `Term` but omitted the required follow-up call to `t.collectMarkerSpecification(boundNames)`. As a result the prepared-statement bind-markers map was never updated for the term, so executing the prepared statement with a bound value produced an error because the marker had no recorded type specification.

### Before / After

```java
// BEFORE – marker specification never collected after prepare()
Term t = newRel.getValue().prepare(keyspace(), receiver);
((SingleColumnRestriction.Contains) existingRestriction).add(t, isKey);
```

```java
// AFTER – marker specification collected so bind markers are registered
Term t = newRel.getValue().prepare(keyspace(), receiver);
t.collectMarkerSpecification(boundNames);
((SingleColumnRestriction.Contains) existingRestriction).add(t, isKey);
```

---

## How to Spot

Every `Term` produced during restriction preparation should have `collectMarkerSpecification` called on it in the same code path where it is created. A reviewer should flag any call to `term.prepare(...)` that is not followed by a corresponding `collectMarkerSpecification` call.

---

## Test

`ContainsRelationTest` was updated to use parameterised queries (bind markers) for all `CONTAINS` and `CONTAINS KEY` assertions, directly exercising the previously broken code path.
