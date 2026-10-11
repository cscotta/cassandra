# Bug 0186: recordsPerBatch silently ignored on non-vectorized readers

**Commit**: 406016bf2a60e25db121ee4895f1008037664dc7 · **Ticket**: #15701

## Detection Rule

When a builder method that configures a mode-specific behavior (e.g., batch size) is called on a builder that may operate in a non-batch mode, verify that the method either throws when the mode is incompatible or documents that the setting is a no-op.

## Code Smell

A `recordsPerBatch()` method on a format reader builder that unconditionally forwards the value to an internal builder without first checking whether batch/vectorized reading is actually enabled for that reader instance.

## Tags

- scope: universal
- category: silent-skip
- code-pattern: config-silently-ignored
- severity: silent-wrong-result
- subsystem: orc/parquet format model reader

## Root Cause

`ORCFormatModel` and `ParquetFormatModel` each expose a `recordsPerBatch()` method on their `ReadBuilder`, but the method forwarded the batch size unconditionally regardless of whether vectorized (batch) reading was active (`isBatchReader`). When a non-vectorized reader was used, the caller's explicit configuration was silently swallowed — no error, no warning, and the setting had no effect.

## Example

```java
// BEFORE — in ORCFormatModel / ParquetFormatModel
@Override
public ReadBuilder<D, S> recordsPerBatch(int numRowsPerBatch) {
    internal.recordsPerBatch(numRowsPerBatch);
    return this;
}
```

```java
// AFTER
@Override
public ReadBuilder<D, S> recordsPerBatch(int numRowsPerBatch) {
    if (!isBatchReader) {
        throw new UnsupportedOperationException(
            "Batch reading is not supported in non-vectorized reader");
    }
    internal.recordsPerBatch(numRowsPerBatch);
    return this;
}
```
