# Bug 0512: LZ4 JVM Crash on Corrupt Decompression Stream

**Commit**: f80a2ce10c · **Ticket**: CASSANDRA-15556
**Subsystem**: `transport/frame/compress/LZ4Compressor.java`
**Category**: Logic error in condition

---

## Root Cause

The native LZ4 `fastDecompressor` variant used by Cassandra does not perform bounds checking during decompression: if given corrupt input it writes beyond the output buffer boundary, causing a JVM crash via a segmentation fault or memory corruption. This is by design — the "fast" decompressor trades safety for speed, and its contract requires the caller to guarantee the declared decompressed length is accurate. Corrupt network frames can violate this contract, triggering a fatal SIGSEGV inside the JVM. The fix upgrades both the lz4-java library (1.4.0 → 1.7.1) and switches to `LZ4SafeDecompressor`, which validates output bounds before writing and also requires the actual input length to be supplied, enabling it to abort gracefully instead of crashing.

### Before / After

```java
// BEFORE
private final LZ4FastDecompressor decompressor;
// ...
decompressor = lz4Factory.fastDecompressor();
// ...
return decompressor.decompress(src, offset, expectedDecompressedLength);
```

```java
// AFTER
private final LZ4SafeDecompressor decompressor;
// ...
decompressor = lz4Factory.safeDecompressor();
// ...
byte[] decompressed = new byte[expectedDecompressedLength];
decompressor.decompress(src, offset, length, decompressed, 0, expectedDecompressedLength);
return decompressed;
```

---

## How to Spot

When a decompressor call does not supply the compressed-input length — only the expected output length — and uses an "unsafe" or "fast" variant, a reviewer should ask what happens when the declared length is wrong. Native decompressors that trust caller-supplied sizes without bounds checking will write out of bounds on corrupt or adversarially crafted input, crashing the JVM rather than throwing a Java exception. The safe rule is: if input comes from an untrusted or potentially corrupt source (network frames, sstable files), use a bounds-checking decompressor variant and supply both input length and maximum output length.

---

## Test

A dedicated test (`ChecksummingWithCorruptedLZ4DoesNotCrashTest`) sends a corrupt LZ4 frame through the transport pipeline and asserts that the JVM survives (throws a Java exception instead of crashing).
