**Commit**: 139e5b15a1 · **Ticket**: KAFKA-17928
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
- Disallow configuring -1 for copier and expiration thread pools dynamically

Co-authored-by: Peter Lee <peterxcli@gmail.com>

Reviewers: Peter Lee <peterxcli@gmail.com>, Satish Duggana <satishd@apache.org>

**Before/After**
```java
// Before
     * @param timeout The timeout of the shutdown.
     * @param timeUnit The time unit of the shutdown timeout.
                                                      long timeout, TimeUnit timeUnit) {
import org.apache.kafka.common.utils.KafkaThread;
import java.util.concurrent.atomic.AtomicInteger;
    private static final String REMOTE_LOG_READER_THREAD_NAME_PREFIX = "remote-log-reader";

// After
import static java.lang.Thread.UncaughtExceptionHandler;


        return createThreadFactory(pattern, daemon, null);
    }

    /**
     * Create a new ThreadFactory.
     *
     * @param pattern       The pattern to use.  If this contains %d, it will be
     *                      replaced with a thread number.  It should not contain more
     *                      than one %d.
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17928: make remote log manager thread-pool configs dynamic (#17859).

**Test**
Test modified in DynamicBrokerConfigTest.scala covering the corrected behaviour.
