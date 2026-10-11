# Bug 2203: CompositeType getString/fromString Break on Component Values Containing ':'

**Commit**: 5d5207b911 · **Ticket**: CASSANDRA-4842
**Subsystem**: src/java/org/apache/cassandra/db/marshal/AbstractCompositeType.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`CompositeType.fromString` split its input on the literal character `':'` using `String.split(":")`, making it impossible to round-trip component values that themselves contain a colon. Similarly, `getString` emitted values without escaping internal colons, producing an ambiguous string. The fix introduced proper escaping (`':'` → `\:`) and unescaping functions, plus a custom `split` routine that honors backslash-escaped delimiters, restoring round-trip fidelity for arbitrary string values.

### Before / After

```java
// BEFORE: split on every ':' unconditionally
String[] parts = source.split(":");
...
ByteBuffer component = type.fromString(part);
```

```java
// AFTER: use escape-aware split and unescape before parsing
List<String> parts = split(source);  // skips '\:' boundaries
...
ByteBuffer component = type.fromString(unescape(part));
```

---

## How to Spot

Any `getString` / `fromString` pair that uses a delimiter-based text format must ensure values are escaped before serialization and unescaped before parsing. Review every call to `String.split` or `indexOf` on a delimiter that could appear inside a field value.

---

## Test

`CompositeTypeTest.testEscapeUnescape` was added; it round-trips composite values containing colons, backslashes, and exclamation marks through `getString` → `fromString` and asserts each component is recovered exactly.
