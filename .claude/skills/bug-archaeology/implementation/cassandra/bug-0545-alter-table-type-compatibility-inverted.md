# Bug 0545: AlterTableStatement Dropped Column Type Compatibility Check Inverted

**Commit**: 08b2192da0 · **Ticket**: CASSANDRA-15203
**Subsystem**: `src/java/org/apache/cassandra/cql3/statements/schema/AlterTableStatement.java`
**Category**: Logic error in condition

---

## Root Cause

The type compatibility guard for re-adding a previously dropped column called `droppedColumn.type.isValueCompatibleWith(type)` — asking whether the old (dropped) type is compatible with the new type — rather than the correct direction `type.isValueCompatibleWith(droppedColumn.type)`, which asks whether the new type can safely read values written under the old type. Because `isValueCompatibleWith` is not symmetric, the check would accept certain unsafe combinations that should have been rejected and reject safe ones, silently allowing data corruption or blocking valid schema changes.

### Before / After

```java
// BEFORE
if (!droppedColumn.type.isValueCompatibleWith(type))
{
    throw ire("Cannot re-add previously dropped column '%s' of type %s, incompatible with previous type %s",
              name, type, droppedColumn.type);
}
```

```java
// AFTER
if (!type.isValueCompatibleWith(droppedColumn.type))
{
    throw ire("Cannot re-add previously dropped column '%s' of type %s, incompatible with previous type %s",
              name, type, droppedColumn.type);
}
```

---

## How to Spot

Whenever a compatibility predicate takes two arguments that are not the same object, read it as a sentence — "can A read data written as B?" — and confirm that the receiver and the argument are assigned the correct roles. Type compatibility checks are especially prone to receiver/argument inversion because the method name alone does not indicate directionality; always map the semantic question ("can the new type decode old on-disk bytes?") to the concrete method call.

---

## Test

No new test added; the fix is a one-line argument-order swap.
