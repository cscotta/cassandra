# Bug 0123: NPE When Assigning a Tuple Literal to a Non-Tuple Target Type

**Commit**: 06f0965a08 · **Ticket**: CASSANDRA-20237
**Subsystem**: src/java/org/apache/cassandra/cql3/terms
**Category**: Missing null / bounds check

---

## Root Cause

In `Tuples.Literal.prepare()`, when the receiver's type was *not* a tuple (e.g. a plain `int` column), the code called `getTupleType(receiver.type)` to obtain a `TupleType` reference and then called `tupleType.size()` before calling `validateTupleAssignableTo`. `getTupleType` returns `null` for non-tuple types, so `tupleType.size()` throws a `NullPointerException` rather than the informative `InvalidRequestException` that `validateTupleAssignableTo` would have emitted. The fix reorders the two calls so validation fires first and the null-dereference is never reached.

### Before / After

```java
// BEFORE – validateTupleAssignableTo called AFTER getTupleType, which returns null
TupleType tupleType = getTupleType(receiver.type);   // null if not a tuple
if (elements.size() != tupleType.size())             // NPE here
    throw invalidRequest(...);
validateTupleAssignableTo(receiver, elements);        // never reached
```

```java
// AFTER – validate first, so non-tuple target throws a clear error immediately
validateTupleAssignableTo(receiver, elements);
TupleType tupleType = getTupleType(receiver.type);
if (elements.size() != tupleType.size())
    throw invalidRequest(...);
```

---

## How to Spot

When a method can return `null` for certain inputs, any dereference of its result should be preceded by a validation call (or a null-guard) that rules out those inputs. A pattern where a type-cast or narrowing accessor is called before the type-check that would have rejected the bad input is a reliable indicator of this class of bug during code review.

---

## Test

`TupleTypeTest` adds an assertion that `SELECT * FROM %s WHERE k = ('a', 'b')` (tuple literal applied to an `int` partition key) emits `"Invalid tuple type literal for k of type int"` rather than a `NullPointerException`.
