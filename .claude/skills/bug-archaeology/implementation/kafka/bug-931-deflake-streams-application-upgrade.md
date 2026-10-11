**Commit**: 261e861340 · **Ticket**: KAFKA-19422
**Subsystem**: tests/kafkatest
**Category**: Race condition / missing synchronization

**Root cause**
In this upgrade test, applications sometimes crash before the upgrade,
so it's actually triggering a bug in several older versions (2.x and
possibly others). It seems to be a rare race condition that has been
happening since 2022. Since we are not going to roll out a patch release
for Kafka Streams 2.x, we should just allow applications to crash before
the upgrade. Reviewers: Matthias J.

**Before/After**
```java
// Before
                self.do_stop_start_bounce(p, None, to_version, counter)
            self.restart_all_nodes_with(to_version)
    def restart_all_nodes_with(self, version):
        self.wait_for_verification(self.processor1, "SMOKE-TEST-CLIENT-CLOSED", self.processor1.STDOUT_FILE)
        self.wait_for_verification(self.processor2, "SMOKE-TEST-CLIENT-CLOSED", self.processor2.STDOUT_FILE)
        self.wait_for_verification(self.processor3, "SMOKE-TEST-CLIENT-CLOSED", self.processor3.STDOUT_FILE)
        self.set_version(self.processor1, version)
        self.set_version(self.processor2, version)
        self.set_version(self.processor3, version)
        kafka_version_str = self.get_version_string(version)
    def do_stop_start_bounce(self, processor, upgrade_from, new_version, counter):
        kafka_version_str = self.get_version_string(new_version)

// After
                self.do_stop_start_bounce(p, None, from_version, to_version, counter)
            self.restart_all_nodes_with(from_version, to_version)
    def restart_all_nodes_with(self, from_version, to_version):
        if from_version.startswith("2."):
            # some older versions crash on shutdown, so we allow crashes here.
            self.wait_for_verification(self.processor1, "SMOKE-TEST-CLIENT-(EXCEPTION|CLOSED)", self.processor1.STDOUT_FILE)
            self.wait_for_verification(self.processor2, "SMOKE-TEST-CLIENT-(EXCEPTION|CLOSED)", self.processor2.STDOUT_FILE)
            self.wait_for_verification(self.processor3, "SMOKE-TEST-CLIENT-(EXCEPTION|CLOSED)", self.processor3.STDOUT_FILE)
        else:
            self.wait_for_verification(self.processor1, "SMOKE-TEST-CLIENT-CLOSED", self.processor1.STDOUT_FILE)
            self.wait_for_verification(self.processor2, "SMOKE-TEST-CLIENT-CLOSED", self.processor2.STDOUT_FILE)
            self.wait_for_verification(self.processor3, "SMOKE-TEST-CLIENT-CLOSED", self.processor3.STDOUT_FILE)
```

**How to spot**
When reviewing tests/kafkatest code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19422: deflake streams_application_upgrade_test (#20004).

**Test**
Test modified in streams_application_upgrade_test.py covering the corrected behaviour.
