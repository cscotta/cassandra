**Commit**: ede289db93 · **Ticket**: KAFKA-17011
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
As part of KIP-584, brokers expose a range of supported versions for each feature. For example, metadata.version might be supported from 1 to 21. (Note that feature level ranges are always inclusive, so this would include both level 1 and 21.)

These supported ranges are supposed to be able to include 0. For example, it should be possible for a broker to support a kraft.version between 0 and 1.

**Before/After**
```java
// Before
import java.util.function.Supplier;
                                                      Supplier<ApiVersionsResponse> apiVersionSupplier) {
                                         Supplier<ApiVersionsResponse> apiVersionSupplier) {
    private final Supplier<ApiVersionsResponse> apiVersionSupplier;
                              Supplier<ApiVersionsResponse> apiVersionSupplier) {

// After
import java.util.function.Function;
                                                      Function<Short, ApiVersionsResponse> apiVersionSupplier) {
                                         Function<Short, ApiVersionsResponse> apiVersionSupplier) {
import java.util.function.Function;
    private final Function<Short, ApiVersionsResponse> apiVersionSupplier;
                              Function<Short, ApiVersionsResponse> apiVersionSupplier) {
import java.util.Objects;
```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17011: fix a bug preventing features from supporting v0 (#16421).

**Test**
Test modified in NetworkClientTest.java covering the corrected behaviour.
