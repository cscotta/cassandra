**Commit**: c771116b89 · **Ticket**: KAFKA-19005
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
jira: https://issues.apache.org/jira/browse/KAFKA-19005

This PR includes following changes:
1. refine the format
2. highligh that it is supported by topic names

<img width="857" alt="999"
src="https://github.com/user-attachments/assets/6eec9e2f-b839-430c-b111-2be3a8538593"
/>

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    // Note that, partitionSizeLimitPerResponse will not be effective if it is larger than the config
    // max.request.partition.size.limit on the server side.

// After
    /**
     * Sets the maximum number of partitions to be returned in a single response.
     * <p>
     * <strong>This option:</strong>
     * <ul>
     *   <li>Is only effective when using topic names (not topic IDs).</li>
     *   <li>Will not be effective if it is larger than the server-side configuration
     *       {@code max.request.partition.size.limit}.
     *   </li>
     * </ul>
     *
     * @param partitionSizeLimitPerResponse the maximum number of partitions per response
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19005 improve the documentation of describetopicsoptions#partitionsizelimitperresponse (#19268).

**Test**
No test added.
