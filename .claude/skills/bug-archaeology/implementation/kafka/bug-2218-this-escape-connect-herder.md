**Commit**: 2353a7c508 · **Ticket**: KAFKA-17723
**Subsystem**: connect (DistributedHerder, MultiThreadedEventProcessor)
**Category**: Logic error in condition

**Root cause**
`DistributedHerder` and `MultiThreadedEventProcessor` both passed `this` to external objects or started threads inside their constructors, triggering JDK 23 `this-escape` warnings. For `MultiThreadedEventProcessor`, the class was made `final` to eliminate the escape. For `DistributedHerder`, the constructors were annotated `@SuppressWarnings("this-escape")` because the architectural cost of refactoring was too high, but the warning was explicitly acknowledged.

**Before/After**
```java
// Before – warning unacknowledged
public class MultiThreadedEventProcessor { ... }

// After – made final to eliminate escape
public final class MultiThreadedEventProcessor { ... }

// DistributedHerder – escape acknowledged
@SuppressWarnings("this-escape")
public DistributedHerder(...) { ... }
```

**How to spot**
Enable `-Xlint:this-escape` and triage each warning: classes that can be made `final` cheaply should be; others should be annotated with a comment explaining why the escape is safe rather than left as unacknowledged noise.

**Test**
No test added.
