**Commit**: 499e167c1e · **Ticket**: KAFKA-19865
**Subsystem**: docs/ops.html
**Category**: Logic error in condition

**Root cause**
[KAFKA-19865](https://issues.apache.org/jira/projects/KAFKA/issues/KAFKA-19865)，
This PR introduces documentation support for monitoring Kafka share
groups and share consumers, including new metric registries and
documentation updates. The changes add new classes to define and expose
share group metrics, and update the documentation to describe these
metrics and how to monitor them. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
  <h4 class="anchor-heading"><a id="kafka_share_group_monitoring" class="anchor-link"></a><a href="#kafka_share_group_monitoring">Share Group Monitoring</a></h4>
  The following set of metrics are available for monitoring the share group:
  <table class="data-table">
    <tbody><tr>
      <th>Metric/Attribute name</th>
      <th>Mbean name</th>
      <th>Description</th>
    </tr>
    <tr>
      <td>TotalShareFetchRequestsPerSec</td>
      <td>kafka.server:type=BrokerTopicMetrics,name=TotalShareFetchRequestsPerSec,topic=([-.\w]+)</td>
      <td>The fetch request rate per second.</td>
```

**How to spot**
When reviewing docs/ops.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19865: document queues metrics changes in ops.html (#21046).

**Test**
No test added.
