# Bug 011: Fix CompressionDictionary being closed while still in use

**Commit**: 21382e6874 · **Ticket**: CASSANDRA-21047
**Subsystem**: Compression / SSTable I/O
**Category**: Resource leak / Race condition / missing synchronization

---

## Root Cause

`ZstdCompressionDictionary` previously implemented `AutoCloseable` and its `close()` method called `selfRef.release()` guarded by an `AtomicBoolean closed` flag. The Caffeine-backed `CompressionDictionaryCache` held the sole lifetime reference and called `dictionary.close()` in its eviction listener. However, `CompressionMetadata` (the SSTable reader) and `CompressionMetadata.Writer` (the SSTable writer) obtained the dictionary object directly but never acquired a counted reference to it—they just stored the pointer. If the cache evicted the dictionary (e.g., during a schema change from Zstd to LZ4) while an SSTable reader or writer was still active, `tidy()` would run and free the underlying native Zstd objects, causing use-after-free when the reader or writer subsequently accessed them.

The fix replaces the bare `AutoCloseable`/`AtomicBoolean` lifecycle with Cassandra's `Ref`/`SelfRefCounted` reference-counting framework. The cache now calls `selfRef().release()` instead of `close()`. `CompressionMetadata` and `Writer` each call `tryRef()` at construction time to acquire their own counted reference, which they release when closed or committed/aborted. The dictionary's native resources are freed exactly once when the global reference count drops to zero.

### Before / After

```java
// BEFORE — cache eviction simply closes the dictionary, ignoring live readers
.removalListener((dictId, dictionary, cause) -> {
    dictionary.close();  // frees native memory even if SSTable still open
})

// BEFORE — CompressionMetadata held raw pointer, no reference acquired
public CompressionMetadata(..., CompressionDictionary compressionDictionary) {
    super(chunkOffsets);  // dictionary not tracked by WrappedSharedCloseable
    this.compressionDictionary = compressionDictionary;
}

// AFTER — cache releases only its own share; native resources survive until last reader closes
.removalListener((dictId, dictionary, cause) -> {
    dictionary.selfRef().release();  // decrement; tidy() runs only when count hits 0
})

// AFTER — CompressionMetadata acquires a reference at construction
private static AutoCloseable[] buildCloseableArray(Memory chunkOffsets,
                                                    CompressionDictionary dictionary) {
    Ref<? extends CompressionDictionary> dictRef = dictionary.tryRef();
    if (dictRef == null) { chunkOffsets.close(); throw ...; }
    return new AutoCloseable[] { chunkOffsets, dictRef::release };
}
```

---

## How to Spot

When a cache holds the only reference to a resource that is also used by long-lived consumers (e.g., open file readers), an eviction callback that unconditionally frees the resource is a use-after-free waiting to happen. The pattern to look for is: cache stores object → eviction listener calls `close()`/`free()` → consumers hold a bare object reference with no counted handle. The correct pattern is reference counting: the cache holds one share, each consumer acquires an additional share at open time and releases it at close time, and the resource is freed only when all shares are released.

---

## Test

`CompressionMetadataTest.testDictionaryRefCountingDuringSchemaChange` creates a dictionary, opens a `CompressionMetadata` instance against it, closes the cache (simulating a schema change), and then asserts that the compressor and decompression dictionary remain accessible through the metadata; it further asserts that only after all metadata instances and the compressor cache are closed does the reference count reach zero and `dictionaryForDecompression` throw `IllegalStateException`.
