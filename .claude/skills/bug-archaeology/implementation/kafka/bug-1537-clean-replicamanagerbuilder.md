**Commit**: 2f1bf2f2ab · **Ticket**: KAFKA-18630
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Christo Lolov <lolovc@amazon.com>

**Before/After**
```java
// Before
import kafka.log.remote.RemoteLogManager;
import kafka.server.AddPartitionsToTxnManager;
import kafka.server.DelayedDeleteRecords;
import kafka.server.DelayedFetch;
import kafka.server.DelayedProduce;
import kafka.server.DelayedRemoteFetch;
import kafka.server.DelayedRemoteListOffsets;
import kafka.server.share.DelayedShareFetch;
import org.apache.kafka.server.purgatory.DelayedOperationPurgatory;
import java.util.Optional;
import scala.jdk.javaapi.OptionConverters;
    private AtomicBoolean isShuttingDown = new AtomicBoolean(false);

// After
import scala.Option;
                             Option.empty(),
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18630: clean replicamanagerbuilder (#18687).

**Test**
No test added.
