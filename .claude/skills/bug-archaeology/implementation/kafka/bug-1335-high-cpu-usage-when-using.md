**Commit**: 8cf2f9a61a · **Ticket**: KAFKA-18046
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This patch is a first step towards resolving KAFKA-18046. Apache Kafka
4.0 ships with log4j2 so the issue raised in the ticket causing high CPU
usage on the fetch path due to LoggerFactory.getLogger() being called on
the handling of all fetch responses is not good. Hence, I propose to fix
that one by caching the Logger used by the `CompletedFetch` class. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Ismael Juma <ismael@juma.me.uk>

**Before/After**
```java
// Before
                        logContext,
import org.apache.kafka.common.utils.LogContext;
    CompletedFetch(LogContext logContext,
        this.log = logContext.logger(CompletedFetch.class);
                logContext,
                logContext,

// After
    // Calling LoggerFactory.getLogger() is pretty expensive with log4j2. See KAFKA-18046 for details.
    // We cache the logger used by CompletedFetch because it is created on every fetch responses.
    private final Logger completedFetchLog;
        this.completedFetchLog = logContext.logger(CompletedFetch.class);
                        completedFetchLog,
    CompletedFetch(Logger log,
        this.log = log;
                logContext.logger(CompletedFetch.class),
                logContext.logger(CompletedFetch.class),
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18046; high cpu usage when using log4j2 (#19138).

**Test**
Test modified in CompletedFetchTest.java covering the corrected behaviour.
