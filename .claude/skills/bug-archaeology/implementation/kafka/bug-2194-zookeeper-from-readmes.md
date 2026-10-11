**Commit**: 54e9d75a3f · **Ticket**: KAFKA-17683
**Subsystem**: README.md
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
### Running a Kafka broker in ZooKeeper mode

Using compiled files:

    ./bin/zookeeper-server-start.sh config/zookeeper.properties
    ./bin/kafka-server-start.sh config/server.properties

>Since ZooKeeper mode is already deprecated and planned to be removed in Apache Kafka 4.0, the docker image only supports running in KRaft mode

Trogdor can run benchmarks and other workloads.  Trogdor can also inject faults in order to stress test the system.
First, we want to start a single-node Kafka cluster with a ZooKeeper and a broker.
Running ZooKeeper:

// After
Trogdor can run benchmarks and other workloads. Trogdor can also inject faults in order to stress test the system.
First, we want to [start a single-node Kafka cluster in KRaft mode](https://github.com/apache/kafka/blob/trunk/README.md#running-a-kafka-broker-in-kraft-mode)
Running Kafka in Kraft mode:
KAFKA_CLUSTER_ID="$(./bin/kafka-storage.sh random-uuid)"
./bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/kraft/server.properties
./bin/kafka-server-start.sh config/kraft/server.properties  &> /tmp/kafka.log &
```

**How to spot**
When reviewing README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17683 remove zookeeper from readmes in 4.0 (#17354).

**Test**
No test added.
