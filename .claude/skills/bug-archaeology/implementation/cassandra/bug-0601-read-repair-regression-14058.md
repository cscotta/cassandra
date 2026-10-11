# Bug 0601: Read Repair Regressions from CASSANDRA-14058 Refactor

**Commit**: eda2613bcc · **Ticket**: CASSANDRA-14353
**Subsystem**: `src/java/org/apache/cassandra/service/reads/`
**Category**: Logic error in condition

---

## Root Cause

CASSANDRA-14058 refactored read repair into a pluggable `ReadRepair` interface with separate `BlockingReadRepair` and `NoopReadRepair` implementations. The refactor introduced several regressions: (1) `BlockingPartitionRepair` was constructed without the `ReadCommand` and `ConsistencyLevel` context it needs to enforce the oversized-mutation guard, so oversized read-repair mutations were silently sent or dropped without proper error handling; (2) the rich assertion context (debug string of all replica responses) that had been inlined in `RowIteratorMergeListener.onMergedRangeTombstoneMarkers` was removed, making assertion failures opaque; and (3) `DataResolver` passed the bare merge listener from `readRepair.getMergeListener` directly to `UnfilteredPartitionIterators.merge` instead of the wrapped version that includes the assertion context, so any RT-merge assertion failure would propagate without the diagnostic information.

### Before / After

```java
// BEFORE — BlockingPartitionRepair missing command and consistency context
public BlockingPartitionRepair(int expectedResponses)
{
    this.responses = new ArrayList<>(expectedResponses);
}

protected AsyncOneResponse sendMutation(InetAddressAndPort endpoint, Mutation mutation)
{
    Tracing.trace("Sending read-repair-mutation to {}", endpoint);
    MessageOut<Mutation> msg = mutation.createMessage(MessagingService.Verb.READ_REPAIR);
    return MessagingService.instance().sendRR(msg, endpoint);
}
```

```java
// AFTER — context passed in; oversized-mutation guard restored
public BlockingPartitionRepair(int expectedResponses, ReadCommand command,
                               ConsistencyLevel consistency)
{
    this.responses = new ArrayList<>(expectedResponses);
    this.command = command;
    this.consistency = consistency;
}

private AsyncOneResponse sendRepairMutation(Mutation mutation, InetAddressAndPort destination)
{
    int mutationSize = (int) Mutation.serializer.serializedSize(mutation, messagingVersion);
    int maxMutationSize = DatabaseDescriptor.getMaxMutationSize();
    if (mutationSize > maxMutationSize)
    {
        // throw ReadTimeoutException or log/drop per DROP_OVERSIZED flag
        ...
    }
    ...
}
```

---

## How to Spot

When refactoring a class that carries important context (command, consistency level) into a new constructor, grep for every call site that constructs the class and verify each one passes the required context. Regressions often arise when a "simple" constructor is left behind for convenience and existing logic is silently skipped rather than failing visibly.

---

## Test

No test added.
