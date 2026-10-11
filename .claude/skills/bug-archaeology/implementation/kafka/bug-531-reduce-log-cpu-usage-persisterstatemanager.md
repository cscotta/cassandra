**Commit**: a2eae88a22 · **Ticket**: KAFKA-19940
**Subsystem**: server-common/src
**Category**: Resource leak

**Root cause**
FYI: https://github.com/apache/kafka/pull/20175/files#r2572693459

This PR optimizes the logging mechanism in PersisterStateManager to
reduce CPU overhead and fix issues preventing dynamic log level
configuration for internal handlers. Replaced the usage of getCanonicalName() with the standard class-based
logger initialization (e.g., LoggerFactory.getLogger(...)). Reviewers: Sushant Mahajan <smahajan@confluent.io>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before

        protected final Logger log;
            String canonicalName = getClass().getCanonicalName();
            if (canonicalName == null) {
                canonicalName = getClass().getName();
            }
            log = LoggerFactory.getLogger(canonicalName);
                log.debug("{} internal topic already exists.", Topic.SHARE_GROUP_STATE_TOPIC_NAME);
                    log.debug("Found coordinator node in cache: {}", node);
            log.debug("Response for RPC {} with key {} is invalid - {}", name(), this.partitionKey, response);
                log.error("Authentication exception", response.authenticationException());
                log.error("Version mismatch exception", response.versionMismatch());

// After

        /**
         * Child classes must provide their own static logger instance. This avoids unnecessary resource usage
         * caused by creating a large number of logger instances.
         *
         * @return child class logger
         */
        protected abstract Logger log();

                log().debug("{} internal topic already exists.", Topic.SHARE_GROUP_STATE_TOPIC_NAME);
                    log().debug("Found coordinator node in cache: {}", node);
            log().debug("Response for RPC {} with key {} is invalid - {}", name(), this.partitionKey, response);
```

**How to spot**
When reviewing server-common/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19940 reduce the log4j2 cpu usage in persisterstatemanager (#21027).

**Test**
Test modified in PersisterStateManagerTest.java covering the corrected behaviour.
