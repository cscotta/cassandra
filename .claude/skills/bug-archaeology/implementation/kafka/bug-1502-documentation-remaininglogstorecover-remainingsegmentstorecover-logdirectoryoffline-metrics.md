**Commit**: cc259d76e9 · **Ticket**: KAFKA-18570
**Subsystem**: docs/ops.html
**Category**: Logic error in condition

**Root cause**
Reviewers: Divij Vaidya <diviv@amazon.com>

**Before/After**
```java
// Before
// (no removals)

// After
      <tr>
        <td>Remaining logs to recover</td>
        <td>kafka.log:type=LogManager,name=remainingLogsToRecover</td>
        <td>The number of remaining logs for each log.dir to be recovered.This metric provides an overview of the recovery progress for a given log directory.</td>
      </tr>
      <tr>
        <td>Remaining segments to recover for the current recovery thread</td>
        <td>kafka.log:type=LogManager,name=remainingSegmentsToRecover</td>
        <td>The number of remaining segments assigned to the currently active recovery thread.</td>
      </tr>
      <tr>
        <td>Log directory offline status</td>
```

**How to spot**
When reviewing docs/ops.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18570: update documentation to add remaininglogstorecover, remainingsegmentstorecover and logdirectoryoffline metr.

**Test**
No test added.
