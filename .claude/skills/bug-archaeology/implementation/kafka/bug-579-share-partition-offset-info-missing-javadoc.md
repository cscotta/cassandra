**Commit**: 17a09f5208 · **Ticket**: KAFKA-19922

**Subsystem**: clients/src/main/java/org/apache/kafka/clients/admin/SharePartitionOffsetInfo.java

**Category**: Missing null/bounds check

**Root cause**
The Javadoc for `SharePartitionOffsetInfo.startOffset()` provided no description of what the start offset actually represents in the context of share groups: it was not obvious that the start offset marks the earliest in-flight record still being evaluated for delivery, and that records between the start offset and the current end may already have completed delivery. Without this clarification, operators using `kafka-share-groups.sh --describe --offsets` could misinterpret the start offset as a committed or high-watermark offset. The fix adds precise Javadoc to the constructor, `startOffset()`, `leaderEpoch()`, and `lag()` methods.

**Before**
```java
// Before: no method-level Javadoc
public long startOffset() {
    return startOffset;
}
```

**After**
```java
// After: clarifying Javadoc added
/**
 * Get the start offset for the share-partition. The start offset is
 * the earliest offset for in-flight records being evaluated for delivery
 * to share consumers. Some records after the start offset may already
 * have completed delivery.
 */
public long startOffset() { return startOffset; }
```

**How to spot**
Check `SharePartitionOffsetInfo` for public getter methods that lack Javadoc, particularly those whose semantics differ subtly from a regular committed-offset concept; missing documentation on share-group offsets is a recurring source of operator confusion.

**Test**
Review the updated HTML operations guide to confirm the start-offset definition is consistent with the Javadoc added in this commit.
