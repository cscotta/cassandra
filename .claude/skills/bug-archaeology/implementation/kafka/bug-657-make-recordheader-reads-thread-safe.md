**Commit**: 8900a18703 · **Ticket**: KAFKA-12999
**Subsystem**: clients/src
**Category**: Missing null / bounds check

**Root cause**
This patch implements

[KIP-1205](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1205%3A+Improve+RecordHeader+to+be+Thread-Safe)
to address concurrency issues (NPE) related to the lazy initialization
of `RecordHeader` fields. The added test verified that concurrent access to `RecordHeader.key()`
and `RecordHeader.value()` no longer throws `NullPointerException`

Reviewers: TaiJuWu <tjwu1217@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
    private String key;
    private ByteBuffer valueBuffer;
    private byte[] value;
            key = Utils.utf8(keyBuffer, keyBuffer.remaining());
            keyBuffer = null;
            value = Utils.toArray(valueBuffer);
            valueBuffer = null;

// After
 * However, each individual {@link org.apache.kafka.common.header.Header} instance
 * is <b>read thread-safe</b>; that is, it is safe for multiple threads to read the same header's key or value concurrently
 * as long as no thread modifies it.
 *
 * <p>
    private volatile String key;
    private volatile ByteBuffer valueBuffer;
    private volatile byte[] value;
            synchronized (this) {
                if (key == null) {
                    key = Utils.utf8(keyBuffer, keyBuffer.remaining());
                    keyBuffer = null;
```

**How to spot**
When reviewing clients/src code, look for missing null / bounds check patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12999 make recordheader reads thread-safe (kip-1205) (#20751).

**Test**
Test modified in RecordHeadersTest.java covering the corrected behaviour.
