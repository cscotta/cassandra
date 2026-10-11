**Commit**: bb300ef757 · **Ticket**: KAFKA-19980
**Subsystem**: docs/configuration
**Category**: Logic error in condition

**Root cause**
see https://github.com/apache/kafka/pull/21125#issuecomment-3664787935,
this is a follow-up to KAFKA-19980. It adds a clarifying comment in `ConfigCommand` to note that the '$'
symbol   must be escaped (e.g., `\$`) when used via the CLI

Reviewers: Kamal Chandraprakash <kamal.chandraprakash@gmail.com>,
 Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
To update the log level for a logger on broker id 0:


    $ bin/kafka-configs.sh --bootstrap-server localhost:9092 --broker-logger 0 --add-config org.apache.kafka.server.quota.ClientQuotaManager\$ThrottledChannelReaper=DEBUG --alter

```

**How to spot**
When reviewing docs/configuration code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: add comment about escaping '$' symbol in configcommand (#21186).

**Test**
No test added.
