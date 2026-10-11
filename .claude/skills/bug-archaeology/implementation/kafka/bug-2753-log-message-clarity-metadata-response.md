**Commit**: b4d77e047b · **Ticket**: KAFKA-4374
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                log.warn("Error while fetching metadata with correlation id {} : {}", requestHeader.correlationId(), errors);

// After
                log.warn("The metadata response from the cluster reported a recoverable issue with correlation id {} : {}", requestHeader.correlationId(), errors);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-4374 update log message for clarity metadata response errors (#16508).

**Test**
No test added.
