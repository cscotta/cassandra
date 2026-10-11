# Bug 1889: Fix assert for counters

**Commit**: 95ac0de7a7 · **Ticket**: CASSANDRA-6175
**Subsystem**: src/java/org/apache/cassandra/net
**Category**: Logic error in condition

---

## Root Cause

`MessagingService.addCallback()` contained an assertion that the outgoing message verb must be `MUTATION`. Counter mutations are sent using the distinct verb `COUNTER_MUTATION`, so registering a callback for a counter write always triggered an `AssertionError`. The guard was simply too narrow: it was written when only one mutation verb existed and was never updated when `COUNTER_MUTATION` was introduced.

### Before / After

```java
// BEFORE
assert message.verb == Verb.MUTATION;
```

```java
// AFTER
assert message.verb == Verb.MUTATION || message.verb == Verb.COUNTER_MUTATION;
```

---

## How to Spot

Assertions that check a single enum value for a type that later gains additional members will silently break when new values are introduced. During review, guard conditions on verb/type enums should be cross-referenced against all call sites to confirm coverage.

---

## Test

No test added.
