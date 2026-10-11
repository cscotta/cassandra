**Commit**: f0a3960e3e · **Ticket**: KAFKA-17867
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            ByteBuffer.wrap(this.data.metrics()) : ClientTelemetryUtils.decompress(this.data.metrics(), cType);
            byte[] compressedPayload;
                compressedPayload = payload.toByteArray();
import org.apache.kafka.common.utils.Utils;
    public static byte[] compress(MetricsData metrics, CompressionType compressionType) throws IOException {
            return Utils.toArray(compressedOut.buffer());
    public static ByteBuffer decompress(byte[] metrics, CompressionType compressionType) {
        ByteBuffer data = ByteBuffer.wrap(metrics);
        try (InputStream in = compression.wrapForInput(data, RecordBatch.CURRENT_MAGIC_VALUE, BufferSupplier.create());
            byte[] bytes = new byte[data.capacity() * 2];
      "name": "Metrics", "type": "bytes", "versions": "0+",

// After
            this.data.metrics() : ClientTelemetryUtils.decompress(this.data.metrics(), cType);
import java.nio.ByteBuffer;
            ByteBuffer compressedPayload;
                compressedPayload = ByteBuffer.wrap(payload.toByteArray());
    public static ByteBuffer compress(MetricsData metrics, CompressionType compressionType) throws IOException {
            return compressedOut.buffer();
    public static ByteBuffer decompress(ByteBuffer metrics, CompressionType compressionType) {
        try (InputStream in = compression.wrapForInput(metrics, RecordBatch.CURRENT_MAGIC_VALUE, BufferSupplier.create());
            byte[] bytes = new byte[metrics.limit() * 2];
      "name": "Metrics", "type": "bytes", "versions": "0+", "zeroCopy": true,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17867 consider using zero-copy for pushtelemetryrequest (#17622).

**Test**
Test modified in PushTelemetryRequestTest.java covering the corrected behaviour.
