**Commit**: 114dbfcaa5 · **Ticket**: KAFKA-20152
**Subsystem**: coordinator-common/src
**Category**: Logic error in condition

**Root cause**
These methods are currently unused, and we should remove them. Reviewers: David Jacot <djacot@confluent.io>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
import com.yammer.metrics.core.MetricsRegistry;
    /**
     * @return The metrics registry.
     */
    public abstract MetricsRegistry registry();

    @Override
    public MetricsRegistry registry() {
        return this.registry;
    }

import com.yammer.metrics.core.MetricsRegistry;

// After
// (no additions)
```

**How to spot**
When reviewing coordinator-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20152 remove unused coordinatormetrics#registry (#21504).

**Test**
No test added.
