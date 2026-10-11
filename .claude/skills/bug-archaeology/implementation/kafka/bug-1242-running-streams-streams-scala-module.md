**Commit**: d619cc5b88 · **Ticket**: KAFKA-19058
**Subsystem**: streams/streams-scala
**Category**: Logic error in condition

**Root cause**
Remove streams-scala module log4j2 file appender. Reviewers: PoAn Yang <payang@apache.org>, Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
      name: A1
    RollingFile:
      - name: R
        fileName: logs/kafka-streams-scala.log
        filePattern: "streams-scala-%d{yyyy-MM-dd}.log"
        PatternLayout:
          pattern: "${logPattern}"
        SizeBasedTriggeringPolicy:
          size: "100KB"
        DefaultRolloverStrategy:
          max: 1
        - ref: R

// After
      name: STDOUT
      PatternLayout:
        pattern: "${logPattern}"
        - ref: STDOUT
```

**How to spot**
When reviewing streams/streams-scala code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19058 running the streams/streams-scala module tests produces a streams-scala.log (#19324).

**Test**
Test modified in log4j2.yaml covering the corrected behaviour.
