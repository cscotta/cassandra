# Bug 0808: CachingParams DEFAULT field not overridable in tests

**Commit**: c0886d8772 · **Ticket**: CASSANDRA-12801
**Subsystem**: Schema / Caching
**Category**: Wrong constant / default value

---

## Root Cause
`CachingParams.DEFAULT` was declared `static final` (package-private), meaning tests could not override it to control caching behaviour per test. The `KeyCacheCqlTest` relied on being able to alter the default caching parameters but could not because the field was both non-public and effectively constant. Changing it to `public static` (non-final) with `@VisibleForTesting` allows tests to swap the value in and out, eliminating the race between test-imposed caching state and node-default state.

### Before / After
```java
// BEFORE
static final CachingParams DEFAULT = new CachingParams(DEFAULT_CACHE_KEYS, DEFAULT_ROWS_PER_PARTITION_TO_CACHE);

// AFTER
@VisibleForTesting
public static CachingParams DEFAULT = new CachingParams(DEFAULT_CACHE_KEYS, DEFAULT_ROWS_PER_PARTITION_TO_CACHE);
```

---

## How to Spot
When a test for a feature that depends on a static default value is flaky, check whether that default is a `static final` field. If so, tests running concurrently cannot substitute their own default without reflection. Making it non-final and `@VisibleForTesting` is the intended pattern here; a cleaner alternative would be a per-instance configuration parameter.

---

## Test
`KeyCacheCqlTest` was extended with explicit default-caching setup; `CQLTester` was also updated to help configure caching per test. The fix allows deterministic caching state in test runs.
