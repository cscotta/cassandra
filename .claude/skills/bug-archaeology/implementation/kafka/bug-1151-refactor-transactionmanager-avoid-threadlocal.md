**Commit**: 8b4560e3f0 · **Ticket**: KAFKA-15767
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
Introduces a concrete subclass of `KafkaThread` named `SenderThread`. The poisoning of the TransactionManager on invalid state changes is
determined by looking at the type of the current thread. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.utils.KafkaThread;
    private final Thread ioThread;
            this.ioThread = new KafkaThread(ioThreadName, this.sender, true);
                  KafkaThread ioThread,
        if (transactionManager != null)
            transactionManager.setPoisonStateOnInvalidTransition(true);


    /**
     * During its normal course of operations, the transaction manager transitions through different internal
     * states (i.e. by updating {@link #currentState}) to one of those defined in {@link State}. These state transitions
     * result from actions on one of the following classes of threads:

// After
    private final Sender.SenderThread ioThread;
            this.ioThread = new Sender.SenderThread(ioThreadName, this.sender, true);
                  Sender.SenderThread ioThread,
import org.apache.kafka.common.utils.KafkaThread;
    public static class SenderThread extends KafkaThread {

        public SenderThread(final String name, Runnable runnable, boolean daemon) {
            super(name, runnable, daemon);
        }
    }
```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15767 refactor transactionmanager to avoid use of threadlocal (#19440).

**Test**
Test modified in KafkaProducerTest.java covering the corrected behaviour.
