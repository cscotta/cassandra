**Commit**: af53758746 · **Ticket**: KAFKA-17814
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
public class OffsetsRequestManager implements RequestManager, ClusterResourceListener {
    @SuppressWarnings("this-escape")
public class ConsumerRebalanceMetricsManager extends RebalanceMetricsManager {
    @SuppressWarnings("this-escape")
public class ShareRebalanceMetricsManager extends RebalanceMetricsManager {
    @SuppressWarnings("this-escape")
public class Metrics implements Closeable {
    @SuppressWarnings("this-escape")

// After
public final class OffsetsRequestManager implements RequestManager, ClusterResourceListener {
public final class ConsumerRebalanceMetricsManager extends RebalanceMetricsManager {
public final class ShareRebalanceMetricsManager extends RebalanceMetricsManager {
public final class Metrics implements Closeable {
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17814 use `final` declaration to replace the suppression `this-escape` (#17613).

**Test**
Test modified in MockAdminClient.java covering the corrected behaviour.
