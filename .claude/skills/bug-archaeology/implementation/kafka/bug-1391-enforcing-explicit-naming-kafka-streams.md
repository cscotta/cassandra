**Commit**: 3ce5f23295 · **Ticket**: KAFKA-18023
**Subsystem**: docs/streams
**Category**: Logic error in condition

**Root cause**
Pull request to implement KIP-1111, aims to add a configuration that
prevents a Kafka Streams application from starting if any of its
internal topics have auto-generated names, thereby enforcing explicit
naming for all internal topics and enhancing the stability of the
application’s topology. - Repartition Topics:

All repartition topics are created in the
KStreamImpl.createRepartitionedSource(...) static method. This method
either receives a name explicitly provided by the user or null and then
builds the final repartition topic name. - Changelog Topics and State Store Names:

There are several scenarios where these are created:
  In the MaterializedInternal constructor.

**Before/After**
```java
// Before
          <tr class="row-odd"><td>log.summary.interval.ms</td>
          <tr class="row-even"><td>enable.metrics.push</td>
          <tr class="row-odd"><td>max.task.idle.ms</td>
          <tr class="row-even"><td>max.warmup.replicas</td>
          <tr class="row-odd"><td>metric.reporters</td>
          <tr class="row-even"><td>metrics.num.samples</td>
          <tr class="row-odd"><td>metrics.recording.level</td>
          <tr class="row-even"><td>metrics.sample.window.ms</td>

// After
              <li><a class="reference internal" href="#ensure-explicit-internal-resource-naming" id="id46">ensure.explicit.internal.resource.naming</a></li>
          <tr class="row-odd"><td>ensure.explicit.internal.resource.naming</td>
            <td>High</td>
            <td colspan="2">
              Whether to enforce explicit naming for all internal resources of the topology, including internal
              topics (e.g., changelog and repartition topics) and their associated state stores.
              When enabled, the application will refuse to start if any internal resource has an auto-generated name.
            </td>
            <td><code class="docutils literal"><span class="pre">false</span></code></td>
          </tr>
          <tr class="row-even"><td>log.summary.interval.ms</td>
          <tr class="row-odd"><td>enable.metrics.push</td>
```

**How to spot**
When reviewing docs/streams code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18023: enforcing explicit naming for kafka streams internal topics (#18233).

**Test**
Test modified in StreamsBuilderTest.java covering the corrected behaviour.
