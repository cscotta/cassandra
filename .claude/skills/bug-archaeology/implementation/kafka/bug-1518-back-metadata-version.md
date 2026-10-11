**Commit**: e6d72c9e60 · **Ticket**: KAFKA-18648
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
During testing, we identified that kafka-python (and aiokafka) relies on metadata request v0 and
hence we need to add these back to comply with the premise of KIP-896 - i.e. it should not
break the clients listed within it. I reverted the changes from #18218 related to the removal of metadata versions 0-3. I will submit a separate PR to undeprecate these API versions on the relevant 3.x branches.

**Before/After**
```java
// Before
  "validVersions": "4-13",
    // Versions 0-3 were removed in Apache Kafka 4.0, Version 4 is the new baseline.
    //
  // Versions 0-3 were removed in Apache Kafka 4.0, Version 4 is the new baseline.
  // Version 1 adds fields for the rack of each broker, the controller id, and
  // whether or not the topic is internal.
  "validVersions": "4-13",
    public void testEmptyMeansEmptyForAllVersions() {
        for (int i = ApiKeys.METADATA.oldestVersion(); i < MetadataRequestData.SCHEMAS.length; i++) {
    for (version <- ApiKeys.METADATA.oldestVersion to ApiKeys.METADATA.l

// After
  "validVersions": "0-13",
    //
  // Version 1 adds fields for the rack of each broker, the controller id, and whether or not the topic is internal.
  //
  "validVersions": "0-13",
        MetadataRequestData metadata = new MetadataRequestData().setTopics(null);
        verifyWriteRaisesNpe((short) 0, metadata);
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
    public void testEmptyMeansAllTopicsV0() {
        MetadataRequestData data = new MetadataRequestData();
        MetadataRequest parsedRequest = new MetadataRequest(data, (short) 0);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18648: add back support for metadata version 0-3 (#18716).

**Test**
Test modified in MessageTest.java covering the corrected behaviour.
