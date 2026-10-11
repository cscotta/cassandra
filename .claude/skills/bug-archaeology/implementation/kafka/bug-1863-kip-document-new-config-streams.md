**Commit**: d208abb38e · **Ticket**: KAFKA-18026
**Subsystem**: docs/streams
**Category**: Logic error in condition

**Root cause**
This PR covers all the docs for KIP-1112, including the new config and a note about the new APIs in the 4.0 section of the upgrade guide. This also fixes/updates some unrelated parts of the upgrade guide that were out-of-date, such as the broker compatibility matrix

Reviewers: Ismael Juma <ismael@juma.me.uk>, Chia-Ping Tsai <chia7712@gmail.com>, Matthias Sax <mjsax@apache.org>

**Before/After**
```java
// Before
          <tr class="row-odd"><td>production.exception.handler</td>
          <tr class="row-even"><td>poll.ms</td>
          <tr class="row-odd"><td>rack.aware.assignment.tags</td>
          <tr class="row-even"><td>replication.factor</td>
          <tr class="row-odd"><td>retry.backoff.ms</td>
          <tr class="row-even"><td>rocksdb.config.setter</td>
          <tr class="row-odd"><td>state.cleanup.delay.ms</td>
          <tr class="row-even"><td>state.dir</td>

// After
              <li><a class="reference internal" href="#processor-wrapper-class" id="id42">processor.wrapper.class</a></li>
          <tr class="row-odd"><td>processor.wrapper.class</td>
            <td>Medium</td>
            <td colspan="2">A class or class name implementing the <code class="docutils literal"><span class="pre">ProcessorWrapper</span></code> interface.
              Must be passed in when creating the topology, and will not be applied unless passed in to the appropriate constructor as a TopologyConfig. You should
              use the <code class="docutils literal"><span class="pre">StreamsBuilder#new(TopologyConfig)</span></code> constructor for DSL applications, and the
              <code class="docutils literal"><span class="pre">Topology#new(TopologyConfig)</span></code> constructor for PAPI applications.</td>
          </tr>
          <tr class="row-even"><td>production.exception.handler</td>
          <tr class="row-odd"><td>poll.ms</td>
          <tr class="row-even"><td>rack.aware.assignment.tags</td>
          <tr class="row-odd"><td>replication.factor</td>
```

**How to spot**
When reviewing docs/streams code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18026: kip-1112, document new config and update the streams upgrade guide (#17906).

**Test**
No test added.
