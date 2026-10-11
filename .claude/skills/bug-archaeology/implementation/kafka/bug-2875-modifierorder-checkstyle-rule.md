**Commit**: 596b945072 · **Ticket**: KAFKA-16643
**Subsystem**: checkstyle/checkstyle.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    final static int RECONNECT_BACKOFF_EXP_BASE = 2;
    final static double RECONNECT_BACKOFF_JITTER = 0.2;
    final static int CONNECTION_SETUP_TIMEOUT_EXP_BASE = 2;
    final static double CONNECTION_SETUP_TIMEOUT_JITTER = 0.2;
    final static int UNKNOWN = -1;
    static public class ReplicaLogDirInfo {
final public class ElectLeadersOptions extends AbstractOptions<ElectLeadersOptions> {
final public class ElectLeadersResult {
    private final static class ListConsumerGroupsResults {
    private final static

// After

    <module name="ModifierOrder"/>

    static final int RECONNECT_BACKOFF_EXP_BASE = 2;
    static final double RECONNECT_BACKOFF_JITTER = 0.2;
    static final int CONNECTION_SETUP_TIMEOUT_EXP_BASE = 2;
    static final double CONNECTION_SETUP_TIMEOUT_JITTER = 0.2;
    static final int UNKNOWN = -1;
    public static class ReplicaLogDirInfo {
public final class ElectLeadersOptions extends AbstractOptions<ElectLeadersOptions> {
public final class ElectLeadersResult {
    private static final class ListConsumerGroupsResults {
```

**How to spot**
When reviewing checkstyle/checkstyle.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16643 add modifierorder checkstyle rule (#15890).

**Test**
Test modified in MockAdminClient.java covering the corrected behaviour.
