**Commit**: 8bdd73c803 · **Ticket**: KAFKA-19137
**Subsystem**: connect/file
**Category**: Wrong serialization / deserialization

**Root cause**
Replace `StandardCharsets.UTF_8.name()` with `StandardCharsets.UTF_8` to
avoid UnsupportedEncodingException and optimize the related code at the
same time. Reviewers: Ken Huang <s7133700@gmail.com>, PoAn Yang
<payang@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                    StandardCharsets.UTF_8.name());
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
        List<String> restNamespace;
        try {
            String encodedSource = encodePath(sourceAndTarget.source());
            String encodedTarget = encodePath(sourceAndTarget.target());
            restNamespace = Arrays.asList(encodedSource, encodedTarget);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unable to create encoded URL paths for source and target using UTF-8", e);
        }
    private static String encodePath(String rawPath) throws UnsupportedEncodingException {

// After
                    StandardCharsets.UTF_8);
        String encodedSource = encodePath(sourceAndTarget.source());
        String encodedTarget = encodePath(sourceAndTarget.target());
        List<String> restNamespace = List.of(encodedSource, encodedTarget);
    private static String encodePath(String rawPath) {
        return URLEncoder.encode(rawPath, StandardCharsets.UTF_8)
        StringEntity entity = new StringEntity(jsonBody, StandardCharsets.UTF_8);
        StringEntity entity = new StringEntity(jsonBody, StandardCharsets.UTF_8);
```

**How to spot**
When reviewing connect/file code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19137 use `standardcharsets.utf_8` instead of `standardcharsets.utf_8.name()` (#19464).

**Test**
Test modified in RestForwardingIntegrationTest.java covering the corrected behaviour.
