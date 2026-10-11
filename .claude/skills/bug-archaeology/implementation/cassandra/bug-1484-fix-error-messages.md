# Bug 1484: Fix CQL Collection Error Messages Using Full Object Instead of Name

**Commit**: 86e3012633 · **Ticket**: n/a
**Subsystem**: cql3 / collection operations
**Category**: Wrong constant / default value

---

## Root Cause

Error message format strings in `Lists.java`, `Maps.java`, `Sets.java`, and `Operation.java` passed the full `ColumnSpecification` (or `ColumnDefinition`) object as the `%s` argument. Because `toString()` on `ColumnSpecification` includes type metadata and other decoration, the messages exposed internal representation noise to users. The fix replaces every such occurrence with `receiver.name` (or `column.name`), which is the plain column name string users actually expect to see in diagnostics.

### Before / After

```java
// BEFORE
throw new InvalidRequestException(
    String.format("Invalid list literal for %s of type %s", receiver, receiver.type.asCQL3Type()));
```

```java
// AFTER
throw new InvalidRequestException(
    String.format("Invalid list literal for %s of type %s", receiver.name, receiver.type.asCQL3Type()));
```

---

## How to Spot

In format strings that incorporate domain objects, always use a specific field (`.name`, `.id`, etc.) rather than relying on the implicit `toString()`. Review all `String.format` / `%s` usages where the argument is a multi-field object — the output may include far more than intended.

---

## Test

No test added.
