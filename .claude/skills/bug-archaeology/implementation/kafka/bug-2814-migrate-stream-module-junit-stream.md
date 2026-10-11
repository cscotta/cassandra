**Commit**: 2d692e4506 · **Ticket**: KAFKA-15623
**Subsystem**: streams/src
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
public abstract class AbstractDualSchemaRocksDBSegmentedBytesStoreTest<S extends Segment> {
    private AbstractDualSchemaRocksDBSegmentedBytesStore<S> bytesStore;
    @Before
    @After

// After
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
public abstract class AbstractDualSchemaRocksDBSegmentedBytesStoreTest {
    private static final String METRICS_SCOPE = "metrics-scope";

    private AbstractDualSchemaRocksDBSegmentedBytesStore<KeyValueSegment> bytesStore;
```

**How to spot**
When reviewing streams/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15623 (3/n) migrate test of stream module to junit5 (stream state) (#16356).

**Test**
Test modified in AbstractDualSchemaRocksDBSegmentedBytesStoreTest.java covering the corrected behaviour.
