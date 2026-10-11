## Commit
363e4ae6dd

## Ticket
KAFKA-20297

## Subsystem
Trogdor / Clients

## Category
State not cleaned up (wrong module placement)

## Root cause
`Scheduler` and `SystemScheduler` lived in `clients/src/main/java/.../common/utils/` but were only used by the `trogdor` module. Having them in `clients` bloated the clients jar and created a coupling between clients and the test-framework module. Moving them to `trogdor` eliminates the dependency.

## Before
```
clients/src/main/java/.../common/utils/Scheduler.java
clients/src/main/java/.../common/utils/SystemScheduler.java
```

## After
```
trogdor/src/main/java/.../trogdor/common/Scheduler.java
trogdor/src/main/java/.../trogdor/common/SystemScheduler.java
```

## How to spot
Check `clients` module for types that have no users inside `clients` itself. Tools like `jdeps` or dependency analysis in Gradle can surface unused public API surfaces.

## Test
`trogdor` tests pass with the new package. A private inner `MockScheduler` was added to the `clients` test that previously used the public one.
