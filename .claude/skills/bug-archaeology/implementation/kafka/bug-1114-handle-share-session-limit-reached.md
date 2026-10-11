**Commit**: fedbb90c12 · **Ticket**: KAFKA-19232
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Handle the new `ShareSessionLimitReachedException` in
`ShareSessionHandler` in the client to reset the ShareSession. Added a
unit test verifying the change. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
                (response.error() == Errors.INVALID_SHARE_SESSION_EPOCH)) {
    @Test
    public void testShareSession() {
        ShareFetchResponse resp3 = ShareFetchResponse.of(Errors.INVALID_SHARE_SESSION_EPOCH, 0, new LinkedHashMap<>(), List.of(), 0);

// After
                (response.error() == Errors.INVALID_SHARE_SESSION_EPOCH) ||
                (response.error() == Errors.SHARE_SESSION_LIMIT_REACHED)) {
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
    @ParameterizedTest
    @EnumSource(value = Errors.class, names = {"INVALID_SHARE_SESSION_EPOCH", "SHARE_SESSION_NOT_FOUND", "SHARE_SESSION_LIMIT_REACHED"})
    public void testShareSession(Errors error) {
        ShareFetchResponse resp3 = ShareFetchResponse.of(error, 0, new LinkedHashMap<>(), List.of(), 0);
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19232: handle share session limit reached exception in clients. (#19619).

**Test**
Test modified in ShareSessionHandlerTest.java covering the corrected behaviour.
