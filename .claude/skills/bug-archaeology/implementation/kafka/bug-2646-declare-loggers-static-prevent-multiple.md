**Commit**: 7c0a96d08d · **Ticket**: KAFKA-17185
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
As discussed in #16657 (comment) , we should make logger as static to avoid creating multiple logger instances. I use the regex private.*Logger.*LoggerFactory to search and check all the results if certain logs need to be static. There are some exceptions that loggers don't need to be static:
1) The logger in the inner class. Since java8 doesn't support static field in the inner class.

**Before/After**
```java
// Before
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Logger log = LoggerFactory.getLogger(Percentiles.class);
    private final Logger log = LoggerFactory.getLogger(OAuthBearerUnsecuredLoginCallbackHandler.class);
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Logger log = LoggerFactory.getLogger(getClass());

// After
    private static final Logger log = LoggerFactory.getLogger(AbstractConfig.class);
    private static final Logger log = LoggerFactory.getLogger(Percentiles.class);
    private static final Logger log = LoggerFactory.getLogger(OAuthBearerUnsecuredLoginCallbackHandler.class);
    private static final Logger log = LoggerFactory.getLogger(KafkaThread.class);
    private static final Logger log = LoggerFactory.getLogger(WorkerSinkTaskContext.class);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17185 declare loggers as static to prevent multiple logger instances (#16680).

**Test**
Test modified in LocalLogManager.java covering the corrected behaviour.
