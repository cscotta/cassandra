**Commit**: 62fe528f4b · **Ticket**: KAFKA-19082
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
This is part of the client side changes required to enable 2PC for
KIP-939

**Producer Config:**
transaction.two.phase.commit.enable The default would be ‘false’. If
set to ‘true’, the broker is informed that the client is participating
in two phase commit protocol and transactions that this client starts
never expire. **Overloaded InitProducerId method**
If the value is 'true' then the corresponding field is set in the
InitProducerIdRequest

Reviewers: Justine Olshan <jolshan@confluent.io>, Artem Livshits
 <alivshits@confluent.io>

**Before/After**
```java
// Before
              files="(AbstractRequest|AbstractResponse|KerberosLogin|WorkerSinkTaskTest|TransactionManagerTest|SenderTest|KafkaAdminClient|ConsumerCoordinatorTest|KafkaAdminClientTest).java"/>
                apiVersions
     * This method does the following:
     * @throws IllegalStateException if no

// After
              files="(AbstractRequest|AbstractResponse|KerberosLogin|WorkerSinkTaskTest|TransactionManagerTest|SenderTest|KafkaAdminClient|ConsumerCoordinatorTest|KafkaAdminClientTest|KafkaProducerTest).java"/>
            final boolean enable2PC = config.getBoolean(ProducerConfig.TRANSACTION_TWO_PHASE_COMMIT_ENABLE_CONFIG);

                apiVersions,
                enable2PC
     * Initialize the transactional state for this producer, similar to {@link #initTransactions()} but
     * with additional capabilities to keep a previously prepared transaction.
     *
     *
     * When {@code keepPreparedTxn} is {@code false}, this behaves like the standard transactional
     * initialization where the method does the following:
     *
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19082: [1/4] add client config for enable2pc and overloaded initproducerid (kip-939) (#19429).

**Test**
Test modified in KafkaProducerTest.java covering the corrected behaviour.
