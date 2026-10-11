**Commit**: 4fd1bedaba · **Ticket**: KAFKA-12392

**Subsystem**: tools/src/main/java/org/apache/kafka/tools/ConsoleProducer.java

**Category**: Wrong constant/default value

**Root cause**
`ConsoleProducer` had two options that both controlled `batch.size`: `--batch-size` and `--max-partition-memory-bytes`. The latter was the original option; `--batch-size` was added later as a cleaner alias, but neither option was documented as the preferred one. KIP-1231 resolves the ambiguity by deprecating `--max-partition-memory-bytes`, adding a runtime warning when it is used, and updating both option descriptions so `--batch-size` is clearly the correct choice going forward.

**Before**
```java
// Before: max-partition-memory-bytes undeprecated, no warning
maxPartitionMemoryBytesOpt = parser.accepts("max-partition-memory-bytes",
    "The buffer size allocated for a partition...");
```

**After**
```java
// After: deprecated annotation + runtime warning
@Deprecated(since = "4.2", forRemoval = true)
maxPartitionMemoryBytesOpt = parser.accepts("max-partition-memory-bytes",
    "(Deprecated) ... Use --batch-size instead.");
if (options.has(maxPartitionMemoryBytesOpt)) {
    System.out.println("Warning: --max-partition-memory-bytes is deprecated...");
}
```

**How to spot**
In `ConsoleProducer.Options`, look for any option whose `CommandLineUtils.maybeMergeOptions` call maps to the same producer config key as another option; the older of the two is a deprecation candidate.

**Test**
Run `ConsoleProducer` with `--max-partition-memory-bytes` and assert that the deprecation warning is printed to stdout before any records are produced.
