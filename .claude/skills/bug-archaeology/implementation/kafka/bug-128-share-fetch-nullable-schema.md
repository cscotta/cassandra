**Commit**: 3c05dfdf0e · **Ticket**: KAFKA-18889
**Subsystem**: clients/src/main/resources/common/message/ShareFetchResponse.json
**Category**: Wrong serialization / deserialization

**Root cause**
The `ShareFetchResponse` JSON schema declared `"nullableVersions": "0+"` for the `Records` field, meaning the generated codec would accept and produce null bytes on the wire for every version. A subsequent patch (KAFKA-18935) had normalised null to `MemoryRecords.EMPTY` at the Java layer, but the schema itself still allowed null to be transmitted. This commit changes the schema to `"nullableVersions": "0"` (empty range), making records non-nullable at the protocol level and aligning it with the `FetchResponse` schema.

**Before/After**
```json
// BEFORE
{ "name": "Records", "type": "records", "versions": "0+",
  "nullableVersions": "0+", "about": "The record data." }

// AFTER
{ "name": "Records", "type": "records", "versions": "0+",
  "nullableVersions": "0", "about": "The record data." }
```

**How to spot**
Compare `nullableVersions` between the `FetchResponse.json` schema (already fixed) and `ShareFetchResponse.json`. Any protocol field whose nullable range is broader than required will silently transmit null on the wire and may cause NPEs in any consumer that does not guard against null. Generated codec stubs from `nullable: "0+"` allow null in every version.

**Test**
`ShareFetchUtilsTest` updated to assert `MemoryRecords.EMPTY` instead of `null` when records are absent. `KafkaApisTest` assertions similarly updated from `assertNull` to `assertEquals(MemoryRecords.EMPTY, ...)`.
