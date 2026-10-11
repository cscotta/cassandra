**Commit**: 036ed569d5 · **Ticket**: KAFKA-19181
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Upon investigations for the failure of the system test
test_broker_failure, it was found there were situations where the
writing of records to the consumer_offsets topic was taking longer than
5 seconds (default value of offsets.commit.timeout.ms). Since the
persister requests of share partition initialization depends on the
completion of the record committing, due to the timeout, there were no
persister requests actually being sent. This PR increases the timeout
for this config to 20 seconds, as a temporary solution. The fix for this
is being tracked in the JIRA -
https://issues.apache.org/jira/browse/KAFKA-19204

Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
                                      use_share_groups=self.use_share_groups)
        use_share_groups=[True]
        use_share_groups=[True]
    def test_broker_failure(self, clean_shutdown, metadata_quorum=quorum.isolated_kraft, num_failed_brokers=1, use_share_groups=True):

// After
        # Set offsets_commit_timeout based on context
        if context.injected_args is not None:
            self.offsets_commit_timeout = context.injected_args.get('offsets_commit_timeout')

                                      use_share_groups=self.use_share_groups,
                                      offsets_commit_timeout=self.offsets_commit_timeout)
{% endif %}

{% if offsets_commit_timeout is defined and offsets_commit_timeout is not none %}
offsets.commit.timeout.ms={{ offsets_commit_timeout }}
        use_share_groups=[True],
        offsets_commit_timeout=[20000]
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19181-2: increased offsets.commit.timeout.ms value as a temporary solution for the system test test_broker_failure.

**Test**
Test modified in kafka.py covering the corrected behaviour.
