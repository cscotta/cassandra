**Commit**: 494a2b9e24 · **Ticket**: KAFKA-20076
**Subsystem**: connect/runtime/src/main/java/org/apache/kafka/connect/runtime/isolation/PluginType.java
**Category**: Wrong serialization / deserialization

**Root cause**
When `PluginInfo` was refactored from a class to a Java record, Jackson's serialization of the `type` field reverted to using the enum's `name()` (uppercase, e.g. `"SINK"`) instead of the overridden `toString()` (lowercase, e.g. `"sink"`). The fix annotates `toString()` with `@JsonValue` so Jackson uses it for both serialization and deserialization, restoring the previous lowercase wire format.

**Before/After**
```java
// Before – toString() is not annotated; Jackson uses name()
@Override
public String toString() {
    return super.toString().toLowerCase(Locale.ROOT);
}

// After
@Override
@JsonValue
public String toString() {
    return super.toString().toLowerCase(Locale.ROOT);
}
```

**How to spot**
When a Java record or class is introduced for a type that was previously serialized as JSON, verify that Jackson picks up the same serialization strategy. Enum values are especially risky because Jackson defaults to `name()` unless `@JsonValue` is present on an overriding method.

**Test**
`PluginInfoTest#testPluginInfoJsonSerialization`: serializes `PluginInfo` records for SINK and SOURCE, asserts the JSON string contains `"type":"sink"` and `"type":"source"`, then round-trips through deserialization and asserts `PluginType.SINK`/`SOURCE`.
