**Commit**: 02b2fa0e3c · **Ticket**: KAFKA-17926
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                                                                            + " Additionally, enabling idempotence requires the value of this configuration to be less than or equal to " + MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION_FOR_IDEMPOTENCE + "."
                                                                            + " If conflicting configurations are set and idempotence is not explicitly enabled, idempotence is disabled. ";

// After
    // The value 5 is aligned with ProducerStateEntry#NUM_BATCHES_TO_RETAIN.
                                                                            + " Additionally, enabling idempotence requires the value of this configuration to be less than or equal to " + MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION_FOR_IDEMPOTENCE + ","
                                                                            + " because broker only retains at most 5 batches for each producer. If the value is more than 5, previous batches may be removed on broker side.";
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17926 improve the documentation explaining why max.in.flight.requests.per.connection should not exceed 5 (#17719).

**Test**
No test added.
