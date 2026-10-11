**Commit**: 6f18f6f335 · **Ticket**: KAFKA-16328
**Subsystem**: docs/streams
**Category**: Resource leak

**Root cause**
- StreamsConfig#RETRIES_CONFIG was deprecated in AK 2.7 and is no longer in use. - StreamsConfig#DEFAULT_WINDOWED_KEY_SERDE_INNER_CLASS and
- StreamsConfig#DEFAULT_WINDOWED_VALUE_SERDE_INNER_CLASS were deprecated in AK 3.0. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
              <li><a class="reference internal" href="#default-windowed-key-serde-inner" id="id32">default.windowed.key.serde.inner (deprecated) </a></li>
              <li><a class="reference internal" href="#default-windowed-value-serde-inner" id="id33">default.windowed.value.serde.inner (deprecated) </a></li>
                <li><code class="docutils literal"><span class="pre">acks=0</span></code> The producer does not wait for acknowledgment from the server and the record is immediately added to the socket buffer and considered sent. No guarantee can be made that the server has received the record in this case, and the <code class="docutils literal"><span class="pre">retries</span></code> configuration will not take effect (as the client won&#8217;t generally know of any failures). The offset returned for each record will always be set to <code class="docutils literal"><span class="pre">-1</span></code>.</li>
          <tr class="row-even"><td>default.windowed.key.serde.inner (Deprecated. Use windowed.inner.class.serde instead.)</td>
            <td>Medium</td>
            <td colspan="2">Default serializer/deserializer for the inner class of windowed keys, implementing the <code class="docutils literal"><span class="pre">Serde</span></code> interface.</td>
            <td><code class="docutils literal"><span class="pre">null</span></code></td>
          </tr>
          <tr class="row-odd"><td>default.windowed.value.serde.inner (Deprecated. Use windowed.inner.class.serde instead.)</td>
            <td>Medium</td>
            <td colspan="2">Default serializer/deserializer for the inner class of windowed values, implementing the <code class="docutils literal"><span class="pre">Serde</span></code> interface.</td>
            <td><code class="docutils literal"><span class="pre">null</span></code></td>

// After
                <li><code class="docutils literal"><span class="pre">acks=0</span></code> The producer does not wait for acknowledgment from the server and the record is immediately added to the socket buffer and considered sent. No guarantee can be made that the server has received the record in this case, and the producer won&#8217;t generally know of any failures. The offset returned for each record will always be set to <code class="docutils literal"><span class="pre">-1</span></code>.</li>
            <td colspan="2">The amount of time in milliseconds, before a request is retried.</td>
```

**How to spot**
When reviewing docs/streams code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16328 remove deprecated config from streamsconfig (#16805).

**Test**
Test modified in StreamsConfigTest.java covering the corrected behaviour.
