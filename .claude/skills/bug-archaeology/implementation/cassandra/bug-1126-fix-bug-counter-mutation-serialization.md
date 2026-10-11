# Bug 1126: AssertionError Thrown After Every Successful Switch Case in ColumnSubselection Serialization

**Commit**: df65a6c68a · **Ticket**: CASSANDRA-10073
**Subsystem**: db/filter/ColumnSubselection, db/marshal/CollectionType
**Category**: Wrong serialization / deserialization

---

## Root Cause

Two independent defects existed in the counter mutation serialization path. First, in `ColumnSubselection.Serializer.serialize`, a `throw new AssertionError()` was placed outside the `switch` statement's cases rather than as the `default` case. Every successful serialization would fall through to this throw after completing the matching case, making it impossible to serialize any `ColumnSubselection`. Second, `CollectionType.validateCellValue` called `valueComparator().validate(cellValue)` instead of `valueComparator().validateCellValue(cellValue)` for multi-cell collections, invoking the wrong validation depth and potentially accepting or rejecting values incorrectly.

### Before / After

```java
// BEFORE – throw is outside switch, always fires
case SLICE: ...; break;
case ELEMENT: ...; break;
}
throw new AssertionError();
```

```java
// AFTER – throw is the default case, fires only on unexpected kind
case SLICE: ...; break;
case ELEMENT: ...; break;
default:
    throw new AssertionError();
}
```

---

## How to Spot

In a `switch` block, a `throw` or `return` that lives after the closing brace rather than inside a `default:` case will execute unconditionally after any matched branch falls through. When reviewing switch statements that must be exhaustive, check that the unreachable-path guard is inside the switch as `default:`, not outside it. A linter or compiler warning about unreachable code after `break` would also catch this pattern.

---

## Test

No test added.
