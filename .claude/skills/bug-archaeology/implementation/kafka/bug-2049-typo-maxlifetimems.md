**Commit**: 207b35901c · **Ticket**: KAFKA-17314
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    private long maxLifeTimeMs = -1;
    public CreateDelegationTokenOptions maxlifeTimeMs(long maxLifeTimeMs) {
        this.maxLifeTimeMs = maxLifeTimeMs;
        return maxLifeTimeMs;
                    .setMaxLifetimeMs(options.maxlifeTimeMs());
        TokenInformation tokenInfo = new TokenInformation(tokenId, options.renewers().get(0), options.renewers(), System.currentTimeMillis(), options.maxlifeTimeMs(), -1);
      val createResult = client.createDelegationToken(new CreateDelegationTokenOptions().renewers(renewer.asJava).maxlifeTimeMs(maxLifeTimeMs))
    val options = new CreateDelegationTokenOptions().maxlifeTimeMs(timeout)
    val createOptions = new CreateDelegationTokenOptions().maxlifeTimeMs(timeout)

// After
    private long maxLifetimeMs = -1;
    /**
     * @deprecated since 4.0 and should not be used any longer.
     */
    @Deprecated
    public CreateDelegationTokenOptions maxlifeTimeMs(long maxLifetimeMs) {
        this.maxLifetimeMs = maxLifetimeMs;
    public CreateDelegationTokenOptions maxLifetimeMs(long maxLifetimeMs) {
        this.maxLifetimeMs = maxLifetimeMs;
        return this;
    }

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17314 fix the typo: `maxlifetimems` (#17749).

**Test**
Test modified in MockAdminClient.java covering the corrected behaviour.
