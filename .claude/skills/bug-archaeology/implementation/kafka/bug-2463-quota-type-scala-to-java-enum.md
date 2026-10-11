**Commit**: 1841c07d4a · **Ticket**: KAFKA-17449
**Subsystem**: server-common/quota/QuotaType
**Category**: Logic error in condition

**Root cause**
`QuotaType` was a sealed Scala trait with case objects (`Fetch`, `Produce`, `Request`, etc.) defined in `kafka.server.QuotaFactory`. Java modules in `core` that needed quota types had to reference Scala companion objects via the `$.MODULE$` pattern (e.g. `QuotaType.RLMFetch$.MODULE$`). By replacing the Scala ADT with a plain Java enum in `server-common`, all modules gain clean `QuotaType.RLM_FETCH`-style references and the `$.MODULE$` hack is eliminated.

**Before/After**
```java
// Before (Java calling Scala companion)
return new RLMQuotaManager(..., QuotaType.RLMFetch$.MODULE$, ...);
new ReplicationQuotaManager(..., AlterLogDirsReplication, ...); // Scala import

// After (Java enum)
return new RLMQuotaManager(..., QuotaType.RLM_FETCH, ...);
new ReplicationQuotaManager(..., QuotaType.ALTER_LOG_DIRS_REPLICATION, ...);
```

**How to spot**
Java files referencing Scala case objects via the `$.MODULE$` accessor pattern signal that a shared type should be migrated to a Java enum or class in a common module. When `QuotaUtils.scala` and `QuotaType` Scala classes are deleted, search for any remaining Scala-style accessor references in Java files to confirm a complete cleanup.

**Test**
No test added (refactoring).
