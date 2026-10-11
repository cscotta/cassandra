# Bug 1531: Fix Scrub Against Collection Type

**Commit**: 110a132b18 · **Ticket**: CASSANDRA-7665
**Subsystem**: src/java/org/apache/cassandra/db, src/java/org/apache/cassandra/db/marshal
**Category**: Wrong serialization / deserialization

---

## Root Cause

During SSTable scrub, `AbstractCell.validateFields()` called `valueValidator.validate(value())` directly on a collection-typed cell. For collection columns the cell value is a single element (the map value or set/list element), not the full serialized collection, so invoking the top-level `validate()` method would crash because the element's encoding does not match the collection's outer serialization format. A new `validateCellValue()` hook was introduced: the base `AbstractType` delegates to `validate()` as before, but `CollectionType` overrides it to call `valueComparator().validate()`, which correctly validates the individual element.

### Before / After

```java
// BEFORE (AbstractCell.validateFields)
AbstractType<?> valueValidator = metadata.getValueValidator(name());
if (valueValidator != null)
    valueValidator.validate(value());
```

```java
// AFTER (AbstractCell.validateFields)
AbstractType<?> valueValidator = metadata.getValueValidator(name());
if (valueValidator != null)
    valueValidator.validateCellValue(value());

// New override in CollectionType
@Override
public void validateCellValue(ByteBuffer cellValue) throws MarshalException
{
    valueComparator().validate(cellValue);
}
```

---

## How to Spot

When validation is applied to a cell value for a type that has a different on-disk representation than its logical type (collections, frozen types, etc.), ensure the validation path understands whether it is examining a whole serialized object or a single decomposed component. Calling a type's full `validate()` on a sub-element is a pattern that silently succeeds or crashes depending on the encoding.

---

## Test

No test added.
