**Commit**: 656242775c · **Ticket**: KAFKA-15307
**Subsystem**: docs/streams
**Category**: Logic error in condition

**Root cause**
Updated Kafka Streams configuration documentation to stay latest with
version 4.0.0. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
            <td><code class="docutils literal"><span class="pre">30000</span></code> (30 seconds)</td>
            <td><code class="docutils literal"><span class="pre">LogAndContinueExceptionHandler</span></code></td>
          <tr class="row-even"><td>default.dsl.store</td>
              [DEPRECATED] The default state store type used by DSL operators. Deprecated in
              favor of <code>dsl.store.suppliers.class</code>
          <tr class="row-odd"><td>retry.backoff.ms</td>
          <tr class="row-even"><td>rocksdb.config.setter</td>
          <tr class="row-odd"><td>state.cleanup.delay.ms</td>
          <tr class="row-even"><td>state.dir</td>
          <tr class="row-odd"><td>task.assignor.class</td>
          <tr class="row-even"><td>task.timeout.ms</td>

// After
            <td><code class="docutils literal"><span class="pre">30000</span></code> (30 seconds) (at-least-once) / <code class="docutils literal"><span class="pre">100</span></code> (exactly-once)</td>
            <td><code class="docutils literal"><span class="pre">LogAndFailExceptionHandler</span></code></td>
          <tr class="row-even"><td>default.dsl.store (Deprecated. Use dsl.store.suppliers.class instead.)</td>
              The default state store type used by DSL operators.
          <tr class="row-odd"><td>repartition.purge.interval.ms</td>
            <td>Low</td>
            <td colspan="2">The frequency in milliseconds with which to delete fully consumed records from repartition topics. Purging will occur after at least this value since the last purge, but may be delayed until later.</td>
            <td><code class="docutils literal"><span class="pre">30000</span></code> (30 seconds)</td>
          </tr>
          <tr class="row-even"><td>retry.backoff.ms</td>
          <tr class="row-odd"><td>rocksdb.config.setter</td>
          <tr class="row-even"><td>state.cleanup.delay.ms</td>
```

**How to spot**
When reviewing docs/streams code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15307: kafka streams configuration docs outdated (#20329).

**Test**
No test added.
