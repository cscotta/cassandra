**Commit**: ccd986dce8 · **Ticket**: KAFKA-20157
**Subsystem**: docs/operations
**Category**: Missing null / bounds check

**Root cause**
Adds documentation for the new RetentionSizeInPercent and
LocalRetentionSizeInPercent metrics introduced in

[KIP-1257](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1257%3A+Partition+Size+Percentage+Metrics+for+Storage+Monitoring). This PR documents metrics added in:
- PR #21468 - Tiered storage metrics (RemoteLogManager)
- PR #21612 - Regular topic metrics (UnifiedLog)

Reviewers: Kamal Chandraprakash <kamal.chandraprakash@gmail.com>

---------

Co-authored-by: manan.gupta <manan.gupta@uber.com>

**Before/After**
```java
// Before
// (no removals)

// After
Partition size as a percentage of retention bytes limit
</td>
<td>

kafka.log:type=Log,name=RetentionSizeInPercent,topic=([-.\w]+),partition=([0-9]+)
</td>
<td>

The partition size expressed as a percentage of the configured retention.bytes limit. Returns 0 for topics with tiered storage enabled (where the metric is reported by RemoteLogManager) or when retention bytes is unlimited. May exceed 100% if retention cleanup is delayed.
</td> </tr>
<tr>
<td>
```

**How to spot**
When reviewing docs/operations code, look for missing null / bounds check patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20157 document kip-1257 partition size percentage metrics (#21613).

**Test**
No test added.
