**Commit**: 95291432fa · **Ticket**: KAFKA-14412
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Instead of reading Task state offsets for non-open Tasks from the
`.checkpoint` file, we now maintain an in-memory cache of the latest
changelog offsets for every Task on the instance. On start-up, this cache is seeded with the changelog offsets for every
on-disk StateStore. Running Active and Standby Tasks then update this
cache on every checkpoint to ensure it always reflects the offsets
on-disk. This breaks the tight coupling between `TaskManager` and `.checkpoint`
files, which will enable us to remove `.checkpoint` files in a later
commit as part of KIP-1035.

**Before/After**
```java
// Before
// (no removals)

// After
    private final StateDirectory stateDirectory;
        this.stateDirectory = stateDirectory;
            stateDirectory.updateTaskOffsets(taskId, changelogOffsets());



            stateDirectory.updateTaskOffsets(taskId, changelogOffsets());

        stateDirectory.updateTaskOffsets(taskId, changelogOffsets());
import org.apache.kafka.streams.state.internals.OffsetCheckpoint;
import java.util.Map;
    private final Map<TaskId, Long> taskOffsetSums = new ConcurrentHashMap<>();
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14412: use shared cache for task offset sums (#20954).

**Test**
Test modified in StateManagerUtilTest.java covering the corrected behaviour.
