## Commit
c48bc49a8f

## Ticket
KAFKA-20245

## Subsystem
Share Groups / DLQ

## Category
Missing null / bounds check

## Root cause
`ShareGroupDLQ` interface had no way to communicate the reason a record was sent to the DLQ. Callers passed arbitrary `Throwable` instances with no semantic meaning. Introducing `ShareGroupDLQThrowable` (with suppressed stack trace for performance) and a set of named singletons (`STALE_BATCH`, `BEHIND_LSO`, `CLIENT_REJECT`, etc.) provides a type-safe, allocation-cheap reason set. The new `ARCHIVING` record state (id=3, per KIP-1191) was also added to bridge `ACKNOWLEDGED` and `ARCHIVED`.

## Before
```java
// RecordState: AVAILABLE, ACQUIRED, ACKNOWLEDGED, ARCHIVED
// No throwable constants; callers passed ad-hoc exceptions
```

## After
```java
// RecordState adds ARCHIVING (byte 3)
Throwable STALE_BATCH = new ShareGroupDLQThrowable("Offset part of stale batch.");
Throwable CLIENT_REJECT = new ShareGroupDLQThrowable("Offset rejected by client.");
// ... additional singletons
```

## How to spot
When a callback interface accepts `Throwable` with no documented contract, look for callers passing throwables with ad-hoc messages. Replacing with named singletons eliminates string allocation and makes reason-handling exhaustive.

## Test
`SharePartitionTest` verifies correct throwable constants are passed for each DLQ reason.
