**Commit**: 5af3547c6e · **Ticket**: KAFKA-18572
**Subsystem**: docs/ops.html
**Category**: Logic error in condition

**Root cause**
Updated the Kafka Streams documentation to include metrics for tasks,
process nodes, and threads that were missing. I was unable to find
metrics such as stream-state-metrics, client-metrics,
state-store-metrics, and record-cache-metrics in the codebase, so they
are not included in this update. Reviewers: Bill Bejeck <bbejeck@apache.org>

**Before/After**
```java
// Before
// (no removals)

// After
      <tr>
        <td>commit-ratio</td>
        <td>The fraction of time the thread spent on committing all tasks</td>
        <td>kafka.consumer:type=consumer-coordinator-metrics,client-id=([-.\w]+)</td>
      </tr>
      <tr>
        <td>poll-ratio</td>
        <td>The fraction of time the thread spent on polling records from consumer</td>
        <td>kafka.consumer:type=consumer-coordinator-metrics,client-id=([-.\w]+)</td>
      </tr>
      <tr>
        <td>poll-records-avg</td>
```

**How to spot**
When reviewing docs/ops.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18572: update kafka streams metric documenation (#18673).

**Test**
No test added.
