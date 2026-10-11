**Commit**: bf660fdeb6 · **Ticket**: KAFKA-18881
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
There are 3 issues (at least) about the multithreaded issue on ConsumerRecords. Hence, it would be better to document it completely. Reviewers: Kirk True <ktrue@confluent.io>, TengYao Chi <kitingiao@gmail.com>, Ken Huang <s7133700@gmail.com>, Xuan-Zhang Gong <gongxuanzhangmelt@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
 * A key/value pair to be received from Kafka. This also consists of a topic name and
 * a partition number from which the record is being received, an offset that points

// After
import java.util.ConcurrentModificationException;
 * A key/value pair to be received from Kafka. This also consists of a topic name and
 * a partition number from which the record is being received, an offset that points
 * <p>
 *
 * <h3>Thread Safety</h3>
 * This consumer record is <b>not thread-safe</b>. Concurrent access to a {@code ConsumerRecord} instance by
 * multiple threads may result in undefined behavior, including but not limited to the following:
 * <ul>
 *   <li>Throwing {@link ConcurrentModificationException} (e.g., when concurrently modifying {@link #headers()}).</li>
 *   <li>Data corruption or logical errors (e.g., inconsistent state of {@code headers} or {@code value}).</li>
 *   <li>Visibility issues (e.g., modifications by one thread not being visible to another thread).</li>
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18881 document the consumerrecord as non-thread safe (#19056).

**Test**
No test added.
