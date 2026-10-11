## Commit
258183c725

## Ticket
KAFKA-20327

## Subsystem
Kafka Streams / State Store Decorators

## Category
Missing override of default method

## Root cause
`AbstractReadOnlyDecorator` and `AbstractReadWriteDecorator` in `processor/internals` were missing `@Override` implementations for many `SessionStore` methods introduced in newer Kafka Streams releases (`findSessions(Instant, Instant)`, `backwardFindSessions(...)`, `fetchSession(K, Instant, Instant)`, `backwardFetch(K)`, etc.). Without overrides, calls to those methods on decorated stores fell through to the default no-op or threw `UnsupportedOperationException`.

## Before
```java
// AbstractReadOnlyDecorator.SessionStoreDecorator
// Missing: findSessions(long, long), fetchSession(K, Instant, Instant),
//          backwardFindSessions variants, backwardFetch variants
```

## After
```java
@Override
public KeyValueIterator<Windowed<K>, AGG> findSessions(long earliestSessionEndTime, long latestSessionEndTime) {
    return wrapped().findSessions(earliestSessionEndTime, latestSessionEndTime);
}
// + 10 more overrides for Instant-based and backward variants
```

## How to spot
After any `SessionStore` API extension, search all decorator classes for missing `@Override` methods. An IDE "implement all methods" action on the interface will surface gaps.

## Test
`AbstractReadOnlyDecoratorTest`, `AbstractReadWriteDecoratorTest` — verify all `SessionStore` methods delegate to the wrapped store.
