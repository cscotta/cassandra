# Bug 2150: Compression Options Silently Ignored When Compression Class Is Null

**Commit**: 4885bfccf1 · **Ticket**: CASSANDRA-5066
**Subsystem**: src/java/org/apache/cassandra/io/compress/CompressionParameters.java
**Category**: Silent skip on non-match

---

## Root Cause

`CompressionParameters.createCompressor()` returned `null` immediately when `compressorClass` was `null` (meaning compression was disabled), without checking whether the caller had also supplied compression options. Any options passed alongside a disabled compressor were silently discarded. This masked configuration mistakes—a user could specify compression tuning parameters (e.g. `chunk_length_kb`) while disabling the compressor and receive no error, leaving those options without effect.

### Before / After

```java
// BEFORE – options ignored silently
if (compressorClass == null)
    return null;
```

```java
// AFTER – error if options present but no compressor
if (compressorClass == null)
{
    if (!compressionOptions.isEmpty())
        throw new ConfigurationException("Unknown compression options (" + compressionOptions.keySet() + ") since no compression class found");
    return null;
}
```

---

## How to Spot

An early-return guard that silences a whole branch of logic should be reviewed to ensure that inputs intended for that branch are either empty or explicitly rejected. When disabling a feature via a null/empty sentinel, validate that no configuration for the disabled feature was provided.

---

## Test

No test added.
