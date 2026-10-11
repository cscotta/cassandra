**Commit**: 08aadb73df · **Ticket**: KAFKA-19987
**Subsystem**: docs/ops.html
**Category**: Logic error in condition

**Root cause**
This commit adds the new thread-level rebalance listener callback
metrics from KIP-1216 to the Streams Monitoring section in ops.html:

- tasks-revoked-latency-avg and tasks-revoked-latency-max
- tasks-assigned-latency-avg and tasks-assigned-latency-max
- tasks-lost-latency-avg and tasks-lost-latency-max

All metrics are at the INFO recording level and measure time in
milliseconds. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
      <tr>
        <td>tasks-revoked-latency-avg</td>
        <td>The average time in ms taken for tasks-revoked rebalance listener callback.</td>
        <td>kafka.streams:type=stream-thread-metrics,thread-id=([-.\w]+)</td>
      </tr>
      <tr>
        <td>tasks-revoked-latency-max</td>
        <td>The maximum time in ms taken for tasks-revoked rebalance listener callback.</td>
        <td>kafka.streams:type=stream-thread-metrics,thread-id=([-.\w]+)</td>
      </tr>
      <tr>
        <td>tasks-assigned-latency-avg</td>
```

**How to spot**
When reviewing docs/ops.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19987: add metrics from kip-1216 to ops.html (#21130).

**Test**
No test added.
