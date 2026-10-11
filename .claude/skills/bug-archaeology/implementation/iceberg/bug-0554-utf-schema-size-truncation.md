# Bug 0554: Schema serialization crash when schema JSON exceeds 65535 bytes

**Commit**: 3048d772aed6572bce28abbddae661d441058f74 · **Ticket**: #14880

## Detection Rule

When a serializer writes a structured object as a UTF string using `DataOutputView.writeUTF()`, verify that the total encoded byte length of the string cannot exceed 65535 bytes (the 2-byte length prefix limit), and if unbounded inputs are possible (e.g., user-defined schemas with many fields), replace `writeUTF`/`readUTF` with a length-prefixed encoding that uses a 4-byte (int) length field.

## Code Smell

A call to `dataOutputView.writeUTF(someJsonString)` or `dataInputView.readUTF()` in a serializer whose input is an Iceberg schema or other open-ended JSON structure, with no guard on the encoded byte length.

## Tags

- scope: java
- category: wrong-serialization
- code-pattern: serialize-size-mismatch
- severity: crash
- subsystem: flink/sink/dynamic

## Root Cause

`DataOutputView.writeUTF()` encodes the string length as a 2-byte (unsigned short) prefix, capping the maximum encoded UTF-8 length at 65535 bytes. Iceberg schema JSON for wide tables with many fields or long field names can exceed this limit. When it does, Flink throws a `UTFDataFormatException` at serialization time, crashing the streaming job. The fix replaces `writeUTF`/`readUTF` for the schema field with a custom `writeLongUTF`/`readLongUTF` that uses a 4-byte int length prefix, and introduces a versioned serializer snapshot so that existing savepoints written with the old format can still be deserialized by the old code path (`writeLongUTF = false`) during migration.

## Example

```java
// BEFORE
if (writeSchemaAndSpec) {
    dataOutputView.writeUTF(SchemaParser.toJson(toSerialize.schema())); // crashes if JSON > 65535 bytes
    dataOutputView.writeUTF(PartitionSpecParser.toJson(toSerialize.spec()));
}

// BEFORE (read)
if (writeSchemaAndSpec) {
    schema = SchemaParser.fromJson(dataInputView.readUTF()); // crashes on restore
}
```

```java
// AFTER
if (writeSchemaAndSpec) {
    if (writeLongUTF) {
        SerializerHelper.writeLongUTF(dataOutputView, SchemaParser.toJson(toSerialize.schema()));
    } else {
        dataOutputView.writeUTF(SchemaParser.toJson(toSerialize.schema()));
    }
    dataOutputView.writeUTF(PartitionSpecParser.toJson(toSerialize.spec()));
}

// AFTER (read)
if (writeSchemaAndSpec) {
    if (writeLongUTF) {
        schema = SchemaParser.fromJson(SerializerHelper.readLongUTF(dataInputView));
    } else {
        schema = SchemaParser.fromJson(dataInputView.readUTF());
    }
}
```
