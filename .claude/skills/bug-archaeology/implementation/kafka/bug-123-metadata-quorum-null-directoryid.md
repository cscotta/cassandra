**Commit**: 61cb33f347 · **Ticket**: KAFKA-19109
**Subsystem**: tools / MetadataQuorumCommand
**Category**: Wrong constant / default value

**Root cause**
`RaftVoterEndpoint.toString()` rendered the `directoryId` field as the literal string `"null"` when the UUID was `Uuid.ZERO_UUID` (the sentinel for "not set"). The display code used the ternary `directoryId.equals(Uuid.ZERO_UUID) ? "null" : "\"" + directoryId + "\""` and always appended the `"directoryId"` key. The resulting JSON-like output contained a `"directoryId": null` entry for every voter that had no directory ID, which was both inaccurate and noisy.

**Before/After**
```java
// Before — always emits "directoryId": null for ZERO_UUID
sb.append("\"directoryId\": ")
  .append(directoryId.equals(Uuid.ZERO_UUID) ? "null" : "\"" + directoryId + "\"");

// After — omits the field entirely when ZERO_UUID
if (!directoryId.equals(Uuid.ZERO_UUID)) {
    sb.append(", ").append("\"directoryId\": \"").append(directoryId).append("\"");
}
```

**How to spot**
Sentinel values (e.g., `ZERO_UUID`, `-1`, empty string) that represent "absent" should be omitted from display output, not rendered as the string `"null"`. Always check whether the value equals the sentinel before including the field in serialized output.

**Test**
`MetadataQuorumCommandTest` — the regex that matches the `CurrentVoters` output line was updated to not require a `directoryId` field, confirming it is absent when not set.
