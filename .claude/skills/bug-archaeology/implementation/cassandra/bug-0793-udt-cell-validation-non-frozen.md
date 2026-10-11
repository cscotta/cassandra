# Bug 0793: Missing Validation for Non-Frozen UDT Cells

**Commit**: 9cc0c80de2 · **Ticket**: CASSANDRA-12916
**Subsystem**: CQL / Type system / Row validation
**Category**: Logic error in condition

---

## Root Cause
`AbstractCell.validate()` split cell validation into two independent steps — check the value via `column().validateCellValue(value())`, then optionally check the path via `column().validateCellPath(path())` — but this two-step model breaks for non-frozen UDTs. A non-frozen UDT is stored as multiple cells sharing a path (the field index) and a value (the field bytes). Correctly validating such a cell requires both pieces together: the path identifies which field type to use, and the value must conform to that type. The old code validated only the raw bytes against the top-level type, which had no way to dispatch to the correct per-field type, so field-level type errors silently passed or produced confusing exceptions.

### Before / After
```java
// BEFORE (AbstractCell.validate)
if (isTombstone()) {
    if (value().hasRemaining())
        throw new MarshalException("A tombstone should not have a value");
} else {
    column().validateCellValue(value());  // cannot resolve field type for non-frozen UDT
}
if (path() != null)
    column().validateCellPath(path());

// AFTER — push both path + value down into ColumnDefinition.validateCell(Cell)
column().validateCell(this);

// New ColumnDefinition.validateCell dispatch:
if (cell.isTombstone()) {
    if (cell.value().hasRemaining()) throw new MarshalException(...);
    if (cell.path() != null) validateCellPath(cell.path());
} else if (type.isUDT()) {
    ((UserType) type).validateCell(cell);  // uses path to find field type, then validates value
} else {
    type.validateCellValue(cell.value());
    if (cell.path() != null) validateCellPath(cell.path());
}
```

---

## How to Spot
Any place where validation of a compound object is split into independent checks on its parts is suspicious when those parts are semantically dependent. For UDTs the field type is encoded in the cell path, so the two cannot be checked in isolation. In review, look for `validate*` methods that receive only a `ByteBuffer` value without access to the cell's path and ask whether the type system ever needs both to determine validity.

---

## Test
`CellTest.testValidateNonFrozenUDT()` and `testValidateFrozenUDT()` were added, covering valid cells, type mismatches, invalid TTL/local-deletion-time, and invalid cell paths for both the non-frozen (multi-cell) and frozen (single-cell) representations.
