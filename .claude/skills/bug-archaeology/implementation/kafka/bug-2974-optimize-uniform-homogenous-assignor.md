**Commit**: fb566e48bf · **Ticket**: KAFKA-16864
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
This patch optimizes uniform (homogenous) assignor by avoiding creating a copy of all the assignments. Instead, the assignor creates a copy only if the assignment is updated. It is a sort of copy-on-write. This change reduces the overhead of the TargetAssignmentBuilder when ran with the uniform (homogenous) assignor.

**Before/After**
```java
// Before
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Math.min;
 * The optimized uniform assignment builder is used to generate the target assignment for a consumer group with
 * It is optimized since the assignment can be done in fewer, less complicated steps compared to when
 * the subscriptions are different across the members.

// After
 * The homogenous uniform assignment builder is used to generate the target assignment for a consumer group with
public class OptimizedUniformAssignmentBuilder {
    private static final Class<?> UNMODIFIABLE_MAP_CLASS = Collections.unmodifiableMap(new HashMap<>()).getClass();
    private static final Class<?> EMPTY_MAP_CLASS = Collections.emptyMap().getClass();

    /**
     * @return True if the provided map is an UnmodifiableMap or EmptyMap. Those classes are not
     * public hence we cannot use the `instanceof` operator.
     */
    private static boolean isImmutableMap(Map<?, ?> map) {
        return UNMODIFIABLE_MAP_CLASS.isInstance(map) || EMPTY_MAP_CLASS.isInstance(map);
    }
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16864; optimize uniform (homogenous) assignor (#16088).

**Test**
Test modified in AssignmentTestUtil.java covering the corrected behaviour.
