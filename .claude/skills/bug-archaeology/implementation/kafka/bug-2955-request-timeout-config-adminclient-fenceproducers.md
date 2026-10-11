**Commit**: eea9ebf8a3 · **Ticket**: KAFKA-16047
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Use REQUEST_TIMEOUT_MS_CONFIG in AdminClient.fenceProducers, 
or options.timeoutMs if specified, as transaction timeout. No transaction will be started with this timeout, but
ReplicaManager.appendRecords uses this value as its timeout. Use REQUEST_TIMEOUT_MS_CONFIG like a regular producer append
to allow for replication to take place. Co-Authored-By: Adrian Preston <prestona@uk.ibm.com>

**Before/After**
```java
// Before
        FenceProducersHandler handler = new FenceProducersHandler(logContext);
        LogContext logContext
            // Set transaction timeout to 1 since it's only being initialized to fence out older producers with the same transactional ID,
            // and shouldn't be used for any actual record writes
            .setTransactionTimeoutMs(1);
        FenceProducersHandler handler = new FenceProducersHandler(logContext);
        mkSet("foo", "bar", "baz").forEach(transactionalId -> assertLookup(handler, transactionalId));

// After
        FenceProducersHandler handler = new FenceProducersHandler(options, logContext, requestTimeoutMs);
import org.apache.kafka.clients.admin.FenceProducersOptions;
    private final int txnTimeoutMs;
        FenceProducersOptions options,
        LogContext logContext,
        int requestTimeoutMs
        this.txnTimeoutMs = options.timeoutMs() != null ? options.timeoutMs() : requestTimeoutMs;
            // This timeout is used by the coordinator to append the record with the new producer epoch to the transaction log.
            .setTransactionTimeoutMs(txnTimeoutMs);
import org.apache.kafka.clients.admin.FenceProducersOptions;
    private final int requestTimeoutMs = 30000;
    private final FenceProducersOptions options = new FenceProducersOptions();
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16047: use request_timeout_ms_config in adminclient.fenceproducers (#16151).

**Test**
Test modified in FenceProducersHandlerTest.java covering the corrected behaviour.
