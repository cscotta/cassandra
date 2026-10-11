**Commit**: d23a61738a · **Ticket**: KAFKA-17937
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
- Remove AbstractFetcherThreadWithIbp26Test as it tests unsupported IBP
- cleanup AbstractFetcherThreadTest to remove unreachable paths, variables, and code

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.junit.jupiter.api.Assumptions.assumeTrue
  val truncateOnFetch = true
    val mockLeaderEndpoint = new MockLeaderEndPoint(truncateOnFetch = truncateOnFetch, version = version)
    val mockLeaderEndpoint = new MockLeaderEndPoint(truncateOnFetch = truncateOnFetch, version = version)
    val mockLeaderEndpoint = new MockLeaderEndPoint(truncateOnFetch = truncateOnFetch, version = version)
    val mockLeaderEndpoint = new MockLeaderEndPoint(truncateOnFetch = truncateOnFetch, version = version) {
    val mockLeaderEndPoint = new MockLeaderEndPoint(truncateOnFetch = truncateOnFetch, version = version) {
    val mockLeaderEndpoint = new MockLeaderEndPoint(truncateOnFetch = truncateOnFetch, version = version)
    val mockLeaderEndpoint = new MockLeaderEndPoint(truncateOnFetch = truncateOnFetch, version = version)
    val mockLeaderEndpoint = new MockLeaderEndPoint(truncateOnFetch = truncateOnFetch, version = version)

// After
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
    val mockLeaderEndpoint = new MockLeaderEndPoint(version = version)
    val mockLeaderEndpoint = new MockLeaderEndPoint(version = version)
    val mockLeaderEndpoint = new MockLeaderEndPoint(version = version)
    val mockLeaderEndpoint = new MockLeaderEndPoint(version = version) {
    val mockLeaderEndPoint = new MockLeaderEndPoint(version = version) {
    val mockLeaderEndpoint = new MockLeaderEndPoint(version = version)
    val mockLeaderEndpoint = new MockLeaderEndPoint(version = version)
    val mockLeaderEndpoint = new MockLeaderEndPoint(version = version)
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17937 cleanup abstractfetcherthreadtest (#18900).

**Test**
Test modified in AbstractFetcherThreadTest.scala covering the corrected behaviour.
