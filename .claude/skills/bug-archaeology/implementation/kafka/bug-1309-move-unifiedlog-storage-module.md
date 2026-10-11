**Commit**: 759fbbba8b · **Ticket**: KAFKA-14484
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
Rewrite UnifiedLog in Java

Reviewers: Jun Rao <jun@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
              files="(LogLoader|LogValidator|RemoteLogManagerConfig|RemoteLogManager).java"/>
              files="(LocalLog|LogLoader|LogValidator|RemoteLogManager|RemoteIndexCache).java"/>
              files="(LogAppendInfo|LogLoader|RemoteLogManagerConfig).java"/>
        Logger logger = LoggerFactory.getLogger(clazz);
import kafka.log.UnifiedLog;
import java.util.function.Supplier;
import scala.jdk.javaapi.CollectionConverters;

            Supplier<Optional<FileRecords.TimestampAndOffset>> searchLocalLog) {
                    List<LogSegment> segmentsCopy = new ArrayList<>(unifiedLog.logSegments());
            List<LogSegment> segments = CollectionConverters.asJava(log.logSegments(fromOffset, Long.MAX_VALUE).toSeq());

// After
              files="(LogLoader|LogValidator|RemoteLogManagerConfig|RemoteLogManager|UnifiedLog).java"/>
              files="(LocalLog|LogLoader|LogValidator|RemoteLogManager|RemoteIndexCache|UnifiedLog).java"/>
              files="(LogAppendInfo|LogLoader|RemoteLogManagerConfig|UnifiedLog).java"/>
    <suppress checks="(ClassDataAbstractionCoupling|ClassFanOutComplexity)"
              files="(UnifiedLog).java"/>
        return logger(LoggerFactory.getLogger(clazz));
    }

    public Logger logger(String clazz) {
        return logger(LoggerFactory.getLogger(clazz));
    }

```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14484: move unifiedlog to storage module (#19030).

**Test**
Test modified in DeleteTopicTest.java covering the corrected behaviour.
