**Commit**: a880c846ae · **Ticket**: KAFKA-17722
**Subsystem**: server (BrokerTopicMetrics)
**Category**: Logic error in condition

**Root cause**
`BrokerTopicMetrics` registered metrics inside its constructor by passing `this` to metric sensor/tag helpers before the object was fully constructed. JDK 23 reports this as a `this-escape` warning because a subclass or external observer could observe a partially initialized object. The fix makes the class `final` so no subclass can observe the partially initialized state and the compiler can verify no escape occurs.

**Before/After**
```java
// Before – non-final class, this-escape in constructor
public class BrokerTopicMetrics {
    BrokerTopicMetrics(KafkaMetricsGroup group) {
        this.group = group;
        group.newMeter(this, ...); // 'this' escapes
    }

// After
public final class BrokerTopicMetrics {
    // same body, 'final' suppresses escape
```

**How to spot**
Enable `-Xlint:this-escape` (JDK 21+) in the build; any class that passes `this` to an external method in its constructor and is not `final` is a candidate for this pattern. Either make the class `final` or move the registration to a factory method.

**Test**
No test added.
