## Commit

- Hash: 8b72204bfd
- Tickets: KAFKA-15443
- Subject: KAFKA-15443: Upgrade RocksDB from 7.9.2 to 9.7.3 and remove removed APIs

## Subsystem

streams / state stores / RocksDB

## Category

Wrong constant / default value

## Root cause

RocksDB 9.x removed several APIs that Kafka Streams was still calling: `AccessHint` enum (used in `setAccessHintOnCompactionIndex`), `NO_FILE_CLOSES` ticker stat, and several deprecated `Options` setters. Additionally, `setLogger(Logger)` changed its parameter type to `LoggerInterface`. Remaining on 7.9.2 prevented Kafka Streams from receiving RocksDB performance improvements and security fixes in the 8.x/9.x line.

## Before

```java
options.setAccessHintOnCompactionIndex(AccessHint.NONE); // removed in 9.x
stats.getTickerCount(TickerType.NO_FILE_CLOSES);         // removed ticker
```

## After

```java
// AccessHint call removed entirely
// NO_FILE_CLOSES: numberOfOpenFiles() now always returns -1
options.setLogger((LoggerInterface) kafkaLogger);        // updated type
```

`gradle/dependencies.gradle`: `rocksDB = "7.9.2"` → `"9.7.3"`.

## How to spot

Dependency version pins significantly behind the current major release that reference removed or deprecated API members — the compilation errors on upgrade are the first signal; the fix is to remove calls to deleted APIs.

## Test

`RocksDBMetricsRecorderTest` and `RocksDBGenericOptionsAdapterTest` updated; Streams state-store integration tests pass with 9.7.3.
