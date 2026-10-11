**Commit**: 5fd7ce2ace · **Ticket**: KAFKA-17414
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.common.MetadataVersion
import kafka.server.{ActionQueue, ReplicaManager, RequestLocal, defaultError, genericError}
      requestLocal = RequestLocal.NoCaching,
                      requestLocal: RequestLocal = RequestLocal.NoCaching): Unit = {
                      requestLocal: RequestLocal = RequestLocal.NoCaching): Unit = {
                         requestLocal: RequestLocal = RequestLocal.NoCaching): Map[String, Errors] = {
                             requestLocal: RequestLocal = RequestLocal.NoCaching,

// After
import org.apache.kafka.server.common.{MetadataVersion, RequestLocal}
import kafka.server.{ActionQueue, ReplicaManager, defaultError, genericError}
import org.apache.kafka.server.common.RequestLocal
      requestLocal = RequestLocal.noCaching,
import org.apache.kafka.server.common.RequestLocal
                      requestLocal: RequestLocal = RequestLocal.noCaching): Unit = {
                      requestLocal: RequestLocal = RequestLocal.noCaching): Unit = {
                         requestLocal: RequestLocal = RequestLocal.noCaching): Map[String, Errors] = {
                             requestLocal: RequestLocal = RequestLocal.noCaching,
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17414 move requestlocal to server-common module (#16986).

**Test**
Test modified in TestRaftRequestHandler.scala covering the corrected behaviour.
