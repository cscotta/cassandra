**Commit**: f75079b276 · **Ticket**: KAFKA-19985
**Subsystem**: docs/ops.html
**Category**: Logic error in condition

**Root cause**
Added documentation for KIP-1071 broker metrics:
- Extended group-count metric to include protocol=streams
- Added streams-group-count metric with states: empty, not_ready,
assigning, reconciling, stable, dead
- Added streams-group-rebalance-rate and streams-group-rebalance-count
metrics

Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
      <td>kafka.server:type=group-coordinator-metrics,name=group-count,protocol={consumer|classic}</td>
      <td>Total number of group per group type: Classic or Consumer</td>

// After
      <td>kafka.server:type=group-coordinator-metrics,name=group-count,protocol={consumer|classic|streams}</td>
      <td>Total number of group per group type: Classic, Consumer or Streams</td>
    <tr>
      <td>Streams Group Count, per state</td>
      <td>kafka.server:type=group-coordinator-metrics,name=streams-group-count,state=[empty|not_ready|assigning|reconciling|stable|dead]</td>
      <td>Total number of Streams Groups in each state: Empty, Not Ready, Assigning, Reconciling, Stable, Dead</td>
    </tr>
    <tr>
      <td>Streams Group Rebalance Rate</td>
      <td>kafka.server:type=group-coordinator-metrics,name=streams-group-rebalance-rate</td>
      <td>The rebalance rate of streams groups</td>
    </tr>
```

**How to spot**
When reviewing docs/ops.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19985: document streams groups metrics changes in ops.html (#21129).

**Test**
No test added.
