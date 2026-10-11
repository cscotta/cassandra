**Commit**: 5441f5e3e1 · **Ticket**: KAFKA-19616
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
issue: [KAFKA-19616](https://issues.apache.org/jira/browse/KAFKA-19616)

**why**: validate log compaction works correctly with compressed data. **what**: adds compression config options to `LogCompactionTester` tool
and extends test coverage to validate log compaction with different
compression types and levels. Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
    def __init__(self, context, kafka, security_protocol="PLAINTEXT", stop_timeout_sec=30, tls_version=None):
        cmd += " --bootstrap-server %s --messages 1000000 --sleep 20 --duplicates 10 --percent-deletes 10" % (self.kafka.bootstrap_servers(self.security_protocol))
    def start_test_log_compaction_tool(self, security_protocol):
        self.compaction_verifier = LogCompactionTester(self.test_context, self.kafka, security_protocol=security_protocol)
    @matrix(metadata_quorum=quorum.all_non_upgrade)
    def test_log_compaction(self, security_protocol='PLAINTEXT', metadata_quorum=quorum.zk):
        self.start_test_log_compaction_tool(security_protocol)
        wait_until(lambda: self.compaction_verifier.is_done, timeout_sec=180, err_msg="Timed out waiting to complete compaction")

// After
    def __init__(self, context, kafka, security_protocol="PLAINTEXT", stop_timeout_sec=30, tls_version=None, compression_config={}):
        self.compression_config = compression_config
        cmd += " --bootstrap-server %s --messages 1000000 --sleep 20 --duplicates 10 --percent-deletes 10" % self.kafka.bootstrap_servers(self.security_protocol)

        if 'type' in self.compression_config:
            cmd += " --compression-type %s" % self.compression_config['type']
        if 'level' in self.compression_config:
            cmd += " --compression-level %s" % self.compression_config['level']
    def start_test_log_compaction_tool(self, security_protocol, compression_config={}):
        self.compaction_verifier = LogCompactionTester(self.test_context, self.kafka, security_protocol=security_protocol, compression_config=compression_config)
    @matrix(metadata_quorum=quorum.all_non_upgrade, compression_config=[
        {},
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19616 add compression type and level support to logcompactiontester (#20396).

**Test**
Test modified in log_compaction_tester.py covering the corrected behaviour.
