**Commit**: b47e2bbed8 · **Ticket**: KAFKA-19155
**Subsystem**: docs/security.html
**Category**: Logic error in condition

**Root cause**
We need to add the correct ACLs for the streams-related RPCs in
docs/security.html. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
        <tr>
            <td>STREAMS_GROUP_HEARTBEAT (88)</td>
            <td>Read</td>
            <td>Group</td>
            <td></td>
        </tr>
        <tr>
            <td>STREAMS_GROUP_HEARTBEAT (88)</td>
            <td>Describe</td>
            <td>Topic</td>
            <td>Required for all source topics and internal topics used in the topology of the group.</td>
        </tr>
```

**How to spot**
When reviewing docs/security.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19155: update docs/security.html for streams-related rpcs (#19887).

**Test**
No test added.
