**Commit**: 16cc877533 · **Ticket**: KAFKA-16448
**Subsystem**: docs/streams
**Category**: Logic error in condition

**Root cause**
Updated docs for KIP-1033. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
          <tr class="row-even"><td>processing.guarantee</td>
          <tr class="row-odd"><td>poll.ms</td>
          <tr class="row-even"><td>rack.aware.assignment.tags</td>
          <tr class="row-odd"><td>replication.factor</td>
          <tr class="row-even"><td>retry.backoff.ms</td>
          <tr class="row-odd"><td>rocksdb.config.setter</td>
          <tr class="row-even"><td>state.cleanup.delay.ms</td>
          <tr class="row-odd"><td>state.dir</td>
          <tr class="row-even"><td>task.assignor.class</td>

// After
              <li><a class="reference internal" href="#processing-exception-handler" id="id41">processing.exception.handler</a></li>
          <tr class="row-even"><td>processing.exception.handler</td>
            <td>Medium</td>
            <td colspan="2">Exception handling class that implements the <code class="docutils literal"><span class="pre">ProcessingExceptionHandler</span></code> interface.</td>
            <td><code class="docutils literal"><span class="pre">LogAndFailProcessingExceptionHandler</span></code></td>
          </tr>
          <tr class="row-odd"><td>processing.guarantee</td>
          <tr class="row-even"><td>poll.ms</td>
          <tr class="row-odd"><td>rack.aware.assignment.tags</td>
          <tr class="row-even"><td>replication.factor</td>
          <tr class="row-odd"><td>retry.backoff.ms</td>
          <tr class="row-even"><td>rocksdb.config.setter</td>
```

**How to spot**
When reviewing docs/streams code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16448: update documentation (#16776).

**Test**
No test added.
