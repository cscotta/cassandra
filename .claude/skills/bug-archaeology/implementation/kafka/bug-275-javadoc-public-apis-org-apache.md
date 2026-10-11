**Commit**: 67850b0b68 · **Ticket**: KAFKA-20136
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
### Summary
This PR adds missing Javadocs to public APIs within the
`org.apache.kafka.clients.consumer` package to improve documentation
coverage and consistency with project standards. ### Changes
- Added missing Javadocs to **public classes, methods, and fields** in
18 files. - Focused on ensuring all public APIs are properly documented according
to Kafka guidelines. ### Test Plan
- Verified that the Javadoc formatting follows existing Kafka
conventions.

**Before/After**
```java
// Before

     * @param timeout the maximum time to wait for the consumer to close.
     * @return a new {@code CloseOptions} instance with the specified timeout.
     * @param operation the group membership operation to apply. Must be one of {@code LEAVE_GROUP}, {@code REMAIN_IN_GROUP},
     * @return a new {@code CloseOptions} instance with the specified group membership operation.
     * @param timeout the maximum time to wait for the consumer to close. If {@code null}, the default timeout will be used.
     * @return this {@code CloseOptions} instance.
     * @param operation the group membership operation to apply. Must be one of {@code LEAVE_GROUP}, {@code REMAIN_IN_GROUP}, or {@code DEFAULT}.
     * @return this {@code CloseOptions} instance.

// After
    /** The unique identifier for this acknowledge type. */
    /**
     * Returns the AcknowledgeType for the given identifier.
     *
     * @param id The identifier for the acknowledge type
     * @return The corresponding AcknowledgeType
     * @throws IllegalArgumentException If the ID is not recognized
     */
/**
 * Options for controlling the consumer close behavior.
 * <p>
 * This class allows customization of the close timeout and group membership operation
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20136: add javadoc to public apis in org.apache.kafka.clients.consumer module (#21429).

**Test**
No test added.
