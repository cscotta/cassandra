**Commit**: 87b37a4065 · **Ticket**: KAFKA-14552
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Kafka 4.0 will remove support for zk mode and will require conversion to kraft
before upgrading to 4.0. The minimum kraft version is 3.0 (aka 3.0-IV1). This provides an opportunity to remove exclusively server side protocols versions
that only exist to allow direct upgrades from versions older than 3.0 or that are
used only by zk mode. Since KRaft became production ready in 3.3, we should consider setting the
baseline to 3.3.

**Before/After**
```java
// Before
        for (int i = 0; i < RequestHeaderData.SCHEMAS.length; i++) {
        for (int i = 0; i < ResponseHeaderData.SCHEMAS.length; i++) {
            for (int i = 0; i < requests.length; i++) {
                Schema schema = requests[i];
                if (schema != null) {
                    b.append("<div>");
                    // Version header
                    b.append("<pre>");
                    b.append(key.name);
                    b.append(" Request (Version: ");
                    b.append(i);
                    b.append(") => ");

// After
    /**
     * Returns `true` if there is at least one valid version, `false` otherwise. When `false` is returned, it typically
     * means that the protocol api is no longer supported, but the api key remains assigned to the removed api so we
     * do not accidentally reuse it for a different api.
     */
    public boolean hasValidVersion() {
        return oldestVersion() <= latestVersion();
    }

        for (int i = RequestHeaderData.LOWEST_SUPPORTED_VERSION; i <= RequestHeaderData.HIGHEST_SUPPORTED_VERSION; i++) {
        for (int i = ResponseHeaderData.LOWEST_SUPPORTED_VERSION; i <= ResponseHeaderData.HIGHEST_SUPPORTED_VERSION; i++) {
            for (short version = key.oldestVersion(); version <= key.latestVersion(); version++) {
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14552: assume a baseline of 3.0 for server protocol versions (#18497).

**Test**
Test modified in NodeApiVersionsTest.java covering the corrected behaviour.
