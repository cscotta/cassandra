**Commit**: ef07b5fad1 · **Ticket**: KAFKA-19461

**Subsystem**: Share Groups / Admin API integration tests

**Category**: Missing test coverage

**Root cause**
Share group admin operations (delete offsets, alter offsets, list offsets) were implemented but had no integration-test coverage in `PlaintextAdminIntegrationTest`. Without end-to-end tests exercising the full broker path, regressions in share group offset management could go undetected until production. The fix adds dedicated test methods for each admin operation against a real share group, verifying correct offsets before and after each operation.

**Before/After**
```java
// Before: no share group admin integration tests in PlaintextAdminIntegrationTest

// After: new test methods added
@Test
public void testDeleteShareGroupOffsets() { ... }

@Test
public void testAlterShareGroupOffsets() { ... }

@Test
public void testListShareGroupOffsets() { ... }
```

**How to spot**
Search for admin API operation implementations (e.g., `deleteOffsets`, `alterOffsets`, `listOffsets`) in share-group code paths and check whether corresponding integration tests exist in `PlaintextAdminIntegrationTest` or similar suites; absence is a coverage gap.

**Test**
The newly added integration tests in `PlaintextAdminIntegrationTest` cover delete, alter, and list offsets for share groups end-to-end against an embedded Kafka cluster.
