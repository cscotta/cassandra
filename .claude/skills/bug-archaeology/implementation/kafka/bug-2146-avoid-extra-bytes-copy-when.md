**Commit**: 5311839bd5 · **Ticket**: KAFKA-17847
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            byte[] payload;
                compressedPayload = payload;
        private byte[] createPayload(List<SinglePointMetric> emittedMetrics) {
            return builder.build().toByteArray();
    public static byte[] compress(byte[] raw, CompressionType compressionType) throws IOException {
                out.write(raw);
                out.flush();
        byte[] compressedData = ClientTelemetryUtils.compress(data, compressionType);
import java.nio.charset.StandardCharsets;

// After
            MetricsData payload;
                compressedPayload = payload.toByteArray();
        private MetricsData createPayload(List<SinglePointMetric> emittedMetrics) {
            return builder.build();
    public static byte[] compress(MetricsData metrics, CompressionType compressionType) throws IOException {
                metrics.writeTo(out);
        byte[] compressedData = ClientTelemetryUtils.compress(metricsData, compressionType);
import java.time.Instant;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.MetricsData;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17847 avoid the extra bytes copy when compressing telemetry payload (#17578).

**Test**
Test modified in PushTelemetryRequestTest.java covering the corrected behaviour.
