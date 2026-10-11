**Commit**: 539f466ccb · **Ticket**: KAFKA-17168
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Kuan-Po (Cooper) Tseng <brandboat@gmail.com>, Satish Duggana <satishd@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.utils.LogContext;
    private final Logger logger;
        logger = new LogContext("[" + Thread.currentThread().getName() + "]").logger(RemoteLogReader.class);
            logger.debug("Reading records from remote storage for topic partition {}", fetchInfo.topicPartition);
            logger.error("Error occurred while reading the remote data for {}", fetchInfo.topicPartition, e);
        logger.debug("Finished reading records from remote storage for topic partition {}", fetchInfo.topicPartition);
import org.apache.kafka.common.utils.LogContext;
    private final Logger logger;
        logger = new LogContext("[" + Thread.currentThread().getName() + "]").logger(RemoteStorageThreadPool.class);


// After
import org.slf4j.LoggerFactory;
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteLogReader.class);
            LOGGER.debug("Reading records from remote storage for topic partition {}", fetchInfo.topicPartition);
            LOGGER.error("Error occurred while reading the remote data for {}", fetchInfo.topicPartition, e);
        LOGGER.debug("Finished reading records from remote storage for topic partition {}", fetchInfo.topicPartition);
import org.slf4j.LoggerFactory;
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteStorageThreadPool.class);
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17168 remove the logprefix to print the thread name (#16657).

**Test**
Test modified in RemoteStorageThreadPool.java covering the corrected behaviour.
