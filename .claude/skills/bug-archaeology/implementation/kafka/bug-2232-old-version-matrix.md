**Commit**: be24e1d608 · **Ticket**: KAFKA-17737
**Subsystem**: tests/kafkatest (E2E test matrix)
**Category**: State not cleaned up

**Root cause**
E2E test parametrize decorators still included Kafka versions 0.10.x and 0.11.x in their version matrices. Those ancient versions are no longer supported, binaries are unavailable in the test infrastructure, and including them inflated the test matrix with entries that always failed or were skipped. The stale entries created confusion about which version combinations were actually exercised.

**Before/After**
```python
# Before – includes pre-1.0 versions
@parametrize(from_kafka_version=str(LATEST_0_10_2), ...)
@parametrize(from_kafka_version=str(LATEST_0_11_0), ...)

# After – matrix starts at >= 1.0
# (0.10.x and 0.11.x entries removed)
```

**How to spot**
When a major version is declared end-of-support, search all `@parametrize` and `@matrix` decorators in the test suite for version constants referencing that major version and remove them.

**Test**
No test added.
