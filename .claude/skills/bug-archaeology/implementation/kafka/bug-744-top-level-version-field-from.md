**Commit**: a0640f9517 · **Ticket**: KAFKA-18351
**Subsystem**: docker/examples
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, Sylwester Lachiewicz <slachiewicz@apache.org>, Dávid Szigecsán

**Before/After**
```java
// Before
version: '2'
version: '2'
version: '2'
version: '2'
version: '2'
version: '2'
version: '2'
version: '2'
version: '2'

// After
// (no additions)
```

**How to spot**
When reviewing docker/examples code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18351: remove top-level version field from docker-compose.yml files (#18322).

**Test**
Test modified in docker-compose.yml covering the corrected behaviour.
