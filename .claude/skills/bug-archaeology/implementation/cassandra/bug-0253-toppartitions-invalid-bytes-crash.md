# Bug 0253: `nodetool toppartitions` Crashes on Invalid Partition Key Bytes

**Commit**: 13d495aa7d · **Ticket**: CASSANDRA-17254
**Subsystem**: `db/ColumnFamilyStore.java`
**Category**: Missing null / bounds check

---

## Root Cause

When building the JMX result for `toppartitions`, the code called `counter.getItem().array()` to obtain a raw `byte[]`, then passed that array to `metadata.getKeyValidator().getString(ByteBuffer.wrap(key))`. If the sampled key bytes were invalid for the table's key validator, `getString()` threw an exception which was not caught, causing the entire JMX call to fail intermittently. The root problem was also that `ByteBuffer.array()` returns the backing array of the buffer which may have a non-zero `arrayOffset`, leading to incorrect data being fed to the validator. The fix passes the original `ByteBuffer` directly to both `ByteBufferUtil.bytesToHex()` and `getKeyValidator().getString()`, avoiding the array-copy issue and relying on the existing buffer-aware code paths.

### Before / After

```java
// BEFORE
byte[] key = counter.getItem().array();
result.put(new CompositeDataSupport(COUNTER_COMPOSITE_TYPE, COUNTER_NAMES, new Object[] {
    Hex.bytesToHex(key),
    counter.getCount(),
    counter.getError(),
    metadata.getKeyValidator().getString(ByteBuffer.wrap(key)) }));
```

```java
// AFTER
ByteBuffer key = counter.getItem();
result.put(new CompositeDataSupport(COUNTER_COMPOSITE_TYPE, COUNTER_NAMES, new Object[] {
    ByteBufferUtil.bytesToHex(key),
    counter.getCount(),
    counter.getError(),
    metadata.getKeyValidator().getString(key) }));
```

---

## How to Spot

Calls to `ByteBuffer.array()` are only safe when `hasArray()` is true and `arrayOffset()` is zero; in all other cases use `ByteBufferUtil` helpers that handle the offset correctly. Additionally, any conversion of raw bytes to a human-readable string via a type validator should be wrapped in a try/catch so that a single malformed sample does not abort the entire JMX operation.

---

## Test

No test added; the commit message describes the fix as addressing intermittent JMX failures caused by invalid bytes reaching the validator.
