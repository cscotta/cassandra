# Bug 1441: Avoid NPE When Creating Table With Frozen Sets, Lists

**Commit**: 932b5eb46d · **Ticket**: CASSANDRA-8104
**Subsystem**: src/java/org/apache/cassandra/cql3/CQL3Type.java
**Category**: Missing null / bounds check

---

## Root Cause

The CQL3 type system allows `null` entries inside collection type argument lists (e.g. when a list or set element type is omitted or partially parsed). Two code paths — the `tuple()` factory and the `freeze()` methods on `RawCollection` and `RawTuple` — unconditionally called methods on these entries without a null guard. The resulting `NullPointerException` surfaced during `CREATE TABLE` statements that used `frozen<set<...>>` or `frozen<list<...>>` syntax.

### Before / After

```java
// BEFORE (tuple factory)
if (ts.get(i).isCounter())

// BEFORE (RawCollection.freeze)
keys.freeze();

// BEFORE (RawTuple.freeze)
for (CQL3Type.Raw t : types)
    t.freeze();
```

```java
// AFTER (tuple factory)
if (ts.get(i) != null && ts.get(i).isCounter())

// AFTER (RawCollection.freeze)
if (keys != null)
    keys.freeze();

// AFTER (RawTuple.freeze)
for (CQL3Type.Raw t : types)
    if (t != null)
        t.freeze();
```

---

## How to Spot

Any loop or direct method call on a collection of parsed type objects should be treated with suspicion if the collection can be populated by a grammar-driven parser: parsers may insert `null` placeholders for optional or missing sub-types. A defensive null check before `.isCounter()`, `.freeze()`, or any other instance method call is required wherever the origin of a type reference is a parser result list rather than a validated internal construct.

---

## Test

No test added.
