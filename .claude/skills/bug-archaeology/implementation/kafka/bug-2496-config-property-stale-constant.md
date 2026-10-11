**Commit**: 31f408d6da · **Ticket**: KAFKA-17382
**Subsystem**: tests/kafkatest/services/kafka/config_property.py
**Category**: State not cleaned up

**Root cause**
`config_property.py` contained a `OffsetCommitRequiredAcksProp = "offsets.commit.required.acks"` constant that referenced a broker configuration property which had been removed from `KafkaConfig.scala`. Keeping the stale constant in the system-test configuration file was misleading: any system test that passed this property name to a broker would silently set an unrecognised config or, worse, match a re-used config key with a different meaning. The fix removes the dead constant.

**Before/After**
```python
# Before
OffsetCommitRequiredAcksProp = "offsets.commit.required.acks"  # removed from KafkaConfig

# After: constant removed
```

**How to spot**
After removing a configuration key from broker code, search `config_property.py` and any other Python test constant files for the exact property string. Stale constants in test helpers are often copied into test parametrize decorators, silently injecting invalid configs into broker test instances.

**Test**
No dedicated test; removal verified by ensuring no system test references the deleted constant.
