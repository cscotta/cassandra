**Commit**: e678b4bb7c · **Ticket**: KAFKA-20183
**Subsystem**: server-common — SharePartitionKey
**Category**: Incorrect filtering / wrong query result

**Root cause**
`SharePartitionKey.getInstance(String key)` split the key string on `:` and assumed exactly 3 tokens: `[groupId, topicId, partition]`. A group ID containing a colon (e.g., `"abc:de"`) produced 4+ tokens and was either rejected with `IllegalArgumentException` ("expected 3 tokens") or misparsed — the second colon-delimited segment would be taken as the topic ID instead of the remainder of the group ID. Since the Kafka protocol does not prohibit colons in group IDs, any share consumer group with a colon in its name would fail entirely.

**Before/After**
```java
// Before — fixed 3-token split; fails for groupId with colon
String[] tokens = key.split(":");
if (tokens.length != 3) { throw new IllegalArgumentException(...); }
return new SharePartitionKey(tokens[0], Uuid.fromString(tokens[1]), parseInt(tokens[2]));

// After — parse from the right; groupId is everything before the last two ':'
String[] tokens = key.split(":");
// last = partition, last-1 = topicId, 0..last-2 joined = groupId
String groupId = String.join(":", Arrays.copyOfRange(tokens, 0, last - 1));
```

**How to spot**
Any key format of the form `<variable-content>:<fixed-content>:<fixed-content>` must be parsed *right-to-left* or use a different delimiter strategy if the variable content can itself contain the delimiter. Parsing left-to-right with a fixed split count will silently truncate or misidentify the variable-length field.

**Test**
`SharePartitionKeyTest` — new tests cover: simple key, group ID with one colon, group ID with multiple colons, group ID starting with colon, group ID ending with colon. Also validates that `validate()` delegates to `getInstance()` and that invalid keys still throw `IllegalArgumentException`.
