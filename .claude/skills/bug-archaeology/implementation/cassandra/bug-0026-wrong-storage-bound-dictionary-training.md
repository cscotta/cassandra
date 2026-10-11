# Bug 0026: Wrong Storage-Size Bound Type for Dictionary Training Parameters

**Commit**: ce983cb89c · **Ticket**: CASSANDRA-21194
**Subsystem**: db/compression, io/compress, tools/nodetool
**Category**: Wrong constant / default value

---

## Root Cause

Three separate validation sites for the dictionary-training parameters `max_dictionary_size` and `max_total_sample_size` used `DataStorageSpec.IntKibibytesBound` to parse and range-check user-supplied values. `IntKibibytesBound` interprets the integer component as kibibytes, so its maximum representable value is `Integer.MAX_VALUE * 1024` bytes — far larger than an `int` can express. The correct bound class is `IntBytesBound`, which interprets the value as bytes and rejects anything larger than `Integer.MAX_VALUE` bytes. Using the kibibyte bound effectively disabled the upper-range guard, allowing values such as `5GiB` to pass validation and later overflow or misbehave when stored as an `int`.

### Before / After

```java
// BEFORE (IDictionaryCompressor, CompressionDictionaryManager, CompressionDictionaryCommandGroup)
new DataStorageSpec.IntKibibytesBound(resolvedValue).toBytes();
```

```java
// AFTER
new DataStorageSpec.IntBytesBound(resolvedValue).toBytes();
```

---

## How to Spot

When validating a numeric configuration value that will ultimately be stored in a primitive `int`, ensure the `DataStorageSpec` bound class matches the expected unit — `IntBytesBound` for byte-valued fields, `IntKibibytesBound` for kibibyte-valued fields. A mismatch silently widens the accepted range by 1024×, letting oversized inputs pass validation.

---

## Test

`CompressionDictionaryIntegrationTest.testMaxBoundForTrainingParameters` and `testInvalidTableCreation` assert that values larger than `Integer.MAX_VALUE` bytes (e.g. `5GiB`) are rejected with an `IllegalArgumentException`.
