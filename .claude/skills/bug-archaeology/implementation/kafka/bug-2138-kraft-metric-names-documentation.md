**Commit**: 71f4001a21 · **Ticket**: KAFKA-17874
**Subsystem**: docs/ops.html
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
    <td>kafka.server:type=raft-metrics,name=current-state</td>
    <td>kafka.server:type=raft-metrics,name=current-leader</td>
    <td>kafka.server:type=raft-metrics,name=current-vote</td>
    <td>kafka.server:type=raft-metrics,name=current-epoch</td>
    <td>kafka.server:type=raft-metrics,name=high-watermark</td>
    <td>kafka.server:type=raft-metrics,name=log-end-offset</td>
    <td>kafka.server:type=raft-metrics,name=number-unknown-voter-connections</td>
    <td>kafka.server:type=raft-metrics,name=commit-latency-avg</td>
    <td>kafka.server:type=raft-metrics,name=commit-latency-max</td>
    <td>kafka.server:type=raft-metrics,name=election-latency-avg</td>
    <td>kafka.server:type=raft-metrics,name=election-latency-max</td>
    <td>kafka.server:type=raft-metrics,name=fetch-records-rate</td>

// After
    <td>kafka.server:type=raft-metrics</td>
    <td>kafka.server:type=raft-metrics</td>
    <td>kafka.server:type=raft-metrics</td>
    <td>kafka.server:type=raft-metrics</td>
    <td>kafka.server:type=raft-metrics</td>
    <td>kafka.server:type=raft-metrics</td>
    <td>kafka.server:type=raft-metrics</td>
    <td>kafka.server:type=raft-metrics</td>
    <td>kafka.server:type=raft-metrics</td>
    <td>kafka.server:type=raft-metrics</td>
    <td>kafka.server:type=raft-metrics</td>
    <td>kafka.server:type=raft-metrics</td>
```

**How to spot**
When reviewing docs/ops.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17874: fix the kraft metric names in the documentation (#17599).

**Test**
No test added.
