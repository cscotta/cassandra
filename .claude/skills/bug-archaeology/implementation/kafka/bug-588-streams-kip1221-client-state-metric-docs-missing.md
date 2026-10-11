**Commit**: 5ab7f0b47b · **Ticket**: KAFKA-19734

**Subsystem**: docs/streams/upgrade-guide.html

**Category**: Missing null/bounds check

**Root cause**
The Kafka Streams upgrade guide did not document the new `client-state` metric introduced by KIP-1221, which now carries `process-id` and `application-id` as additional tags. Operators upgrading from an older version would have no way to know that existing monitoring dashboards needed to be updated to include these new tag dimensions. The fix adds an entry in the upgrade guide describing the new tags and updates `ops.html` to reflect that the `client-state` metric description has changed.

**Before**
```html
<!-- Before: no mention of KIP-1221 client-state metric changes in upgrade guide -->
```

**After**
```html
<!-- After: upgrade-guide entry explains new process-id and application-id tags -->
<p>The <code>client-state</code> metric now includes
  <code>process-id</code> and <code>application-id</code> tags...</p>
```

**How to spot**
After adding new metric tags to an existing metric, check whether the Streams upgrade guide has a corresponding entry; if not, operators relying on the metric in dashboards or alerts will be silently broken after upgrade.

**Test**
Read the rendered `upgrade-guide.html` and confirm the KIP-1221 `client-state` section accurately describes all new tags added to the metric.
