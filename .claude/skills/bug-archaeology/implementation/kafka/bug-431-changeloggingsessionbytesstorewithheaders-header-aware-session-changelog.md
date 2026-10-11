**Commit**: e0d1d232f8 · **Ticket**: KAFKA-20158
**Subsystem**: streams/src
**Category**: Race condition / missing synchronization

**Root cause**
This covers:
- New `ChangeLoggingSessionBytesStoreWithHeaders` class (overrides put
to extract headers from serialized `AggregationWithHeaders` and log them
separately)
- New `ChangeLoggingSessionBytesStoreWithHeadersTes`t (6 tests)
- `rawAggregation()` static method added to
`AggregationWithHeadersDeserializer`
- `internalContext` visibility change in
`ChangeLoggingSessionBytesStore`

Reviewers Uladzislau Blok, Alieh Saeedi <asaeedi@confluent.io> , TengYao
Chi <frankvicky@apache.org>

**Before/After**
```java
// Before
    private InternalProcessorContext<?, ?> internalContext;

// After
    /**
     * Extract the raw aggregation bytes from serialized AggregationWithHeaders,
     * stripping the headers prefix.
     */
    static byte[] rawAggregation(final byte[] aggregationWithHeaders) {
        if (aggregationWithHeaders == null) {
            return null;
        }

        final ByteBuffer buffer = ByteBuffer.wrap(aggregationWithHeaders);
        readHeaders(buffer);
        return readBytes(buffer, buffer.remaining());
```

**How to spot**
When reviewing streams/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20158: add changeloggingsessionbytesstorewithheaders for header-aware session changelog logging   (3/n) (#21567).

**Test**
Test modified in ChangeLoggingSessionBytesStoreWithHeadersTest.java covering the corrected behaviour.
