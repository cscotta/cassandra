## Commit
1b69c09ced

## Ticket
KAFKA-15630

## Subsystem
MirrorMaker / Connect

## Category
Wrong constant / default value

## Root cause
`MirrorSourceConfig.OFFSET_LAG_MAX_DOC` was a one-line placeholder description that failed to explain the purpose, units, or implications of the `offset.lag.max` configuration. Operators had no documentation to guide tuning.

## Before
```java
private static final String OFFSET_LAG_MAX_DOC = "Limit of low-water mark offset lag.";
```

## After
```java
private static final String OFFSET_LAG_MAX_DOC =
    "Determines the point at which the source consumer position is considered " +
    "sufficiently caught up with the end of a source topic partition. " +
    "When the offset lag is at or below this threshold, the partition will not " +
    "be mirrored until it falls behind again. The default value of " +
    MirrorSourceConfig.OFFSET_LAG_MAX_DEFAULT + " effectively disables this optimization.";
```

## How to spot
Look for `*_DOC` constants that are single-sentence stubs without units, defaults, or behavioral caveats. Any config that controls throttling or thresholds especially needs a clear unit statement.

## Test
Documentation change only; verified by reading the rendered config documentation.
