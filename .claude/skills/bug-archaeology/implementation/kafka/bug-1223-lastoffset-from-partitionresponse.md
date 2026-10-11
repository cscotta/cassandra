**Commit**: fcf6da0a0d · **Ticket**: KAFKA-19098
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
The `lastOffset` is not used actually, so it can be removed. Reviewers: Jhen-Yung Hsu <jhenyunghsu@gmail.com>, Ken Huang
<s7133700@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import static org.apache.kafka.common.requests.ProduceResponse.INVALID_OFFSET;

                            INVALID_OFFSET,
        public long lastOffset;
            this(error, baseOffset, INVALID_OFFSET, logAppendTime, logStartOffset, recordErrors, errorMessage, new ProduceResponseData.LeaderIdAndEpoch());
        }

        public PartitionResponse(Errors error, long baseOffset, long lastOffset, long logAppendTime, long logStartOffset, List<RecordError> recordErrors, String errorMessage) {
            this(error, baseOffset, lastOffset, logAppendTime, logStartOffset, recordErrors, errorMessage, new ProduceResponseData.LeaderIdAndEpoch());
            long lastOffset,
            this.lastOffset = lastOffset;
                    lastOffset == that.lastOffset &&

// After
            this(error, baseOffset, logAppendTime, logStartOffset, recordErrors, errorMessage, new ProduceResponseData.LeaderIdAndEpoch());
            return Objects.hash(error, baseOffset, logAppendTime, logStartOffset, recordErrors, errorMessage, currentLeader);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19098 remove `lastoffset` from partitionresponse (#19398).

**Test**
No test added.
