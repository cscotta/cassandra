**Commit**: 929a90da50 · **Ticket**: KAFKA-18667
**Subsystem**: tests/kafkatest/tests/core/replication_test.py
**Category**: Wrong constant / default value

**Root cause**: `test_replication_with_broker_failure` hardcoded `controller_num_nodes_override = 1` and had no test matrix entry for combined-mode (broker + controller) KRaft clusters with multiple controllers. With only one controller node, a combined-mode failure test could not simulate the scenario where a controller that is also a broker fails while other controllers remain. The fix adds a new `@matrix` entry for `combined_kraft` with `num_controllers=3` and threads `num_controllers` through to `controller_num_nodes_override`.

**Before/After**:
```python
# BEFORE – always 1 controller, no combined-mode test
def test_replication_with_broker_failure(..., metadata_quorum=quorum.zk):
    ...
    controller_num_nodes_override = 1

# AFTER – new combined_kraft matrix entry with 3 controllers
@matrix(failure_mode=["clean_shutdown", "hard_shutdown", "clean_bounce", "hard_bounce"],
        broker_type=["leader"],
        metadata_quorum=[quorum.combined_kraft],
        num_controllers=[3])
def test_replication_with_broker_failure(..., metadata_quorum=quorum.zk, num_controllers=1):
    ...
    controller_num_nodes_override = num_controllers
```

**How to spot**: Test matrices that omit `combined_kraft` as a `metadata_quorum` value will not exercise combined-mode failure scenarios. Search for `quorum.all_non_upgrade` or similar multi-value quorum parameters and verify `combined_kraft` is represented in failure-mode tests.

**Test**: The new test matrix entry runs `clean_shutdown`, `hard_shutdown`, `clean_bounce`, and `hard_bounce` failures for the partition leader in combined-mode KRaft with three controllers, verifying end-to-end replication durability across both security protocols (PLAINTEXT, SASL_SSL).
