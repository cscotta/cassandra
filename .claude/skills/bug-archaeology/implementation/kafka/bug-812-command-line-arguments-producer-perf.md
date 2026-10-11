**Commit**: c565ba1a04 · **Ticket**: KAFKA-19598
**Subsystem**: docs/ops.html
**Category**: Logic error in condition

**Root cause**
This implements KIP-1147 for kafka-producer-perf-test.sh. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
<pre><code class="language-bash">$ bin/kafka-producer-perf-test.sh --topic tieredTopic --num-records 1200 --record-size 1024 --throughput -1 --producer-props bootstrap.servers=localhost:9092</code></pre>
              "--topic %(topic)s --num-records %(num_records)d --record-size %(record_size)d --throughput %(throughput)d --producer-props bootstrap.servers=%(bootstrap_servers)s client.id=%(client_id)s %(metrics_props)s" % args
            stats = new Stats(config.numRecords, isSteadyState);
                    steadyStateStats = new Stats(config.numRecords - config.warmupRecords, isSteadyState);
                // Make sure all messages are sent before printing out the stats and the metrics
            throw new IllegalArgumentException("no payload File Path or record Size or payload-monotonic option provided");
            System.out.println("Number of messages read: " + payloadByteList.size());
                .newArgumentParser("producer-performance")
                        "you can specify a transacti

// After
<pre><code class="language-bash">$ bin/kafka-producer-perf-test.sh --bootstrap-server localhost:9092 --topic tieredTopic --num-records 1200 --record-size 1024 --throughput -1</code></pre>
              "--topic %(topic)s --num-records %(num_records)d --record-size %(record_size)d --throughput %(throughput)d --command-property bootstrap.servers=%(bootstrap_servers)s client.id=%(client_id)s %(metrics_props)s" % args
            stats = new Stats(config.numRecords, config.reportingInterval, isSteadyState);
                    steadyStateStats = new Stats(config.numRecords - config.warmupRecords, config.reportingInterval, isSteadyState);
                // Make sure all records are sent before printing out the stats and the metrics
            throw new IllegalArgumentException("No payload file, record size or payload-monotonic option provided.");
            System.out.println("Number of records read: " + payloadByteList.size());
                .newArgumentParser("kafka-producer-perf-test")
```

**How to spot**
When reviewing docs/ops.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19598: command-line arguments for producer perf test (#20361).

**Test**
Test modified in producer_performance.py covering the corrected behaviour.
