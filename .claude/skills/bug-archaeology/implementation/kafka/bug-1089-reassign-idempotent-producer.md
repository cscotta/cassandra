## Commit

13fa4537f5 — KAFKA-18905: Disable idempotent producer in reassign_partitions_test to avoid KAFKA-9199

## Ticket

KAFKA-18905

## Subsystem

System tests / partition reassignment

## Category

Race condition / missing synchronization

## Root cause

`reassign_partitions_test.py` enabled idempotent producers to improve test realism. However, idempotent producers interact with partition leadership changes in a way that triggers KAFKA-9199: when a leader changes during reassignment, the producer's sequence numbers can become misaligned with the new leader, causing spurious `OutOfOrderSequenceException` errors that abort the producer's transaction. These errors are unrelated to the reassignment logic being tested but caused test failures that were misattributed to reassignment bugs. Temporarily disabling idempotent producers removes the interaction until KAFKA-9199 is resolved.

## Before / After

```python
# Before — idempotent producer enabled, triggers KAFKA-9199
producer_props = {"enable.idempotence": "true"}

# After — idempotent producer disabled pending KAFKA-9199 fix
# TODO: re-enable after KAFKA-9199 is fixed
producer_props = {}
```

## How to spot

When a system test starts failing intermittently after enabling an advanced producer feature (idempotence, transactions), check whether the test exercises partition leadership changes. Leader changes during idempotent-producer sessions can trigger sequence-number misalignment bugs that are separate from the feature under test.

## Test

The existing reassignment test suite is the regression guard; the change suppresses false-positive failures until the root cause (KAFKA-9199) is addressed.
