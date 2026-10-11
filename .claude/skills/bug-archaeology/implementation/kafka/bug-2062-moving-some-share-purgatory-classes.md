**Commit**: 4966aed40d · **Ticket**: KAFKA-17970
**Subsystem**: checkstyle/import-control-share.xml
**Category**: Logic error in condition

**Root cause**
As part of PR: https://github.com/apache/kafka/pull/17636 where purgatory has been moved from core to server-common hence move some existing classes used in Share Fetch to Share module. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.server.share.{DelayedShareFetch, DelayedShareFetchPartitionKey}
import kafka.server.share.{DelayedShareFetch, DelayedS

// After
        <allow pkg="org.apache.kafka.server.purgatory" />
import org.apache.kafka.server.share.fetch.DelayedShareFetchGroupKey;
import org.apache.kafka.server.share.fetch.DelayedShareFetchGroupKey;
import org.apache.kafka.server.share.fetch.DelayedShareFetchKey;
import org.apache.kafka.server.share.fetch.DelayedShareFetchGroupKey;
import org.apache.kafka.server.share.fetch.DelayedShareFetchKey;
import org.apache.kafka.server.share.fetch.DelayedShareFetchPartitionKey;
import kafka.server.share.DelayedShareFetch
import org.apache.kafka.server.share.fetch.DelayedShareFetchPartitionKey
```

**How to spot**
When reviewing checkstyle/import-control-share.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17970 moving some share purgatory classes from core to share module (#17722).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
