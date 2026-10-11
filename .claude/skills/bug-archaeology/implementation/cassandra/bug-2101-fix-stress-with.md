# Bug 2101: Stress Tool Sends Incorrectly Formatted Blob Literals for CQL3

**Commit**: 4cd813632d · **Ticket**: n/a
**Subsystem**: tools/stress/src/org/apache/cassandra/stress/operations/, tools/stress/src/org/apache/cassandra/stress/util/Operation.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

The stress tool's `getUnQuotedCqlBlob` helper returned bare hex digits (e.g. `deadbeef`) for all CQL versions. CQL2 accepted this format, but CQL3 requires blob literals to be prefixed with `0x` (e.g. `0x deadbeef`). As a result, every stress operation that used blob parameters — inserts, reads, range slices, counter operations — would fail or produce garbage results when run against a CQL3 endpoint. Additionally, `queryParamsAsByteBuffer` treated parameter strings as raw bytes instead of hex-encoded bytes, and formatted queries surrounded parameters with single quotes that are not valid for CQL3 blob literals.

### Before / After

```java
// BEFORE
protected String getUnQuotedCqlBlob(byte[] term) {
    return Hex.bytesToHex(term);
}
// in formatCqlQuery: result.append('\'').append(parm).append('\'');
```

```java
// AFTER
protected String getUnQuotedCqlBlob(byte[] term, boolean isCQL3) {
    return isCQL3 ? "0x" + Hex.bytesToHex(term) : Hex.bytesToHex(term);
}
// in formatCqlQuery: result.append(parm);  // no quotes for CQL3 blobs
```

---

## How to Spot

When a utility method produces a format string and is shared by two protocol versions with different literal syntax, the version flag must be threaded through to every call site. The absence of a version-discriminating parameter is a sign that the method was written for one version and later reused without adjustment.

---

## Test

No test added.
