## Commit
ba0738d69a

## Ticket
KAFKA-20251

## Subsystem
Group Coordinator / Config

## Category
Missing null / bounds check

## Root cause
Group-level assignment interval and assignor-offload configs (`consumer.assignment.interval.ms`, `share.assignment.interval.ms`, `streams.assignment.interval.ms`, `*.assignor.offload.enable`) were missing from `GroupConfig`. Without them, operators could only set these values at the broker level; per-group overrides were silently ignored.

## Before
```java
// GroupConfig had no CONSUMER_ASSIGNMENT_INTERVAL_MS_CONFIG,
// SHARE_ASSIGNMENT_INTERVAL_MS_CONFIG, or STREAMS_ASSIGNMENT_INTERVAL_MS_CONFIG entries
```

## After
```java
.define(CONSUMER_ASSIGNMENT_INTERVAL_MS_CONFIG, INT,
    GroupCoordinatorConfig.CONSUMER_GROUP_ASSIGNMENT_INTERVAL_MS_DEFAULT, ...)
.define(SHARE_ASSIGNMENT_INTERVAL_MS_CONFIG, INT, ...)
.define(STREAMS_ASSIGNMENT_INTERVAL_MS_CONFIG, INT, ...)
```

## How to spot
When adding a new broker config that controls per-group behavior, always check whether a corresponding group-level config needs to be added to `GroupConfig`. Missing group-level configs silently prevent per-group tuning.

## Test
`GroupConfigTest` asserts all new keys parse, validate, and clamp correctly.
