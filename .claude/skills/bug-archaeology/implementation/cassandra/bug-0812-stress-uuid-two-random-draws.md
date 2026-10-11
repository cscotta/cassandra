# Bug 0812: cassandra-stress UUID generator uses two independent random draws, breaking reproducibility

**Commit**: 1bc736d935 · **Ticket**: CASSANDRA-12729
**Subsystem**: cassandra-stress / Data Generation
**Category**: Wrong constant / default value

---

## Root Cause
The `UUIDs` stress value generator constructed a `UUID` by calling `identityDistribution.next()` twice — once for the most-significant bits and once for the least-significant bits. Because the distribution advances its internal state on each call, the two halves were drawn from different positions in the sequence, meaning a given seed did not reproducibly produce the same UUID value across runs. The fix draws the seed once and reuses it for both halves, ensuring that the UUID is a deterministic function of a single distribution sample.

### Before / After
```java
// BEFORE — two independent draws; UUID not reproducible from a single seed
return new UUID(identityDistribution.next(), identityDistribution.next());

// AFTER — one draw used for both halves
long seed = identityDistribution.next();
return new UUID(seed, seed);
```

---

## How to Spot
Any generator that claims to produce a reproducible value from a seeded distribution but makes multiple `next()` calls to construct a single logical value is suspect. The extra draws shift the distribution state and make subsequent values unpredictable relative to the declared seed. Review all multi-field data constructors in stress generators for this pattern.

---

## Test
No new test added. The change is a one-line correction to the generation logic.
