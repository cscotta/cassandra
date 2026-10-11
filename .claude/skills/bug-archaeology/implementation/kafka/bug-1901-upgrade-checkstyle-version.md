**Commit**: 2b43c49f51 · **Ticket**: KAFKA-18050
**Subsystem**: checkstyle/checkstyle.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    <module name="UnnecessaryParentheses" />
                   new AddRaftVoterRequestData().
                       setClusterId(options.clusterId().orElse(null)).
                       setTimeoutMs(timeoutMs).
                       setVoterId(voterId) .
                       setVoterDirectoryId(voterDirectoryId).
                       setListeners(listeners));
    private OffsetsForLeaderEpochUtils(){}
                put(PatternType.LITERAL, new HashSet<>());
                put(PatternType.PREFIXED, new HashSet<>());
            }};
                put(PatternType.LITERAL, new HashSet<>());

// After
    <module name="UnnecessaryParentheses">
      <property name="tokens" value="IDENT, NUM_DOUBLE, LAMBDA, TEXT_BLOCK_LITERAL_BEGIN, UNARY_MINUS, UNARY_PLUS, INC, DEC, POST_INC, POST_DEC" />
    </module>
                        new AddRaftVoterRequestData().
                            setClusterId(options.clusterId().orElse(null)).
                            setTimeoutMs(timeoutMs).
                            setVoterId(voterId) .
                            setVoterDirectoryId(voterDirectoryId).
                            setListeners(listeners));
    private OffsetsForLeaderEpochUtils() {}
                    put(PatternType.LITERAL, new HashSet<>());
                    put(PatternType.PREFIXED, new HashSet<>());
```

**How to spot**
When reviewing checkstyle/checkstyle.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18050 upgrade the checkstyle version to 10.20.2 (#17999).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
