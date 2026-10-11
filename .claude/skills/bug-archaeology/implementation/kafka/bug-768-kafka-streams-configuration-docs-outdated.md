**Commit**: 639492cc0a · **Ticket**: KAFKA-15307
**Subsystem**: docs/streams
**Category**: Logic error in condition

**Root cause**
Added new section for window serdes and update Streams configuration
page accordingly. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
              <li><a class="reference internal" href="#deserialization-exception-handler" id="id7">default.deserialization.exception.handler (deprecated since 4.0)</a></li>
              <li><a class="reference internal" href="#production-exception-handler" id="id24">default.production.exception.handler (deprecated since 4.0)</a></li>
              <li><a class="reference internal" href="#windowed-inner-class-serde" id="id38">windowed.inner.class.serde</a></li>
          <tr class="row-odd"><td>window.size.ms</td>
    <div class="section" id="windowed.inner.class.serde">
      <h4><a class="toc-backref" href="#id31">windowed.inner.class.serde</a><a class="headerlink" href="#windowed.inner.class.serde" title="Permalink to this headline"></a></h4>
      <blockquote>
        <div>
          <p>
            Serde for the inner class of a windowed record. Must implement the org.apache.kafka.common.serialization.Serde interface.
          </p>
          <p>

// After
              <li><a class="reference internal" href="#deserialization-exception-handler" id="id7">default.deserialization.exception.handler (deprecated)</a></li>
              <li><a class="reference internal" href="#production-exception-handler" id="id24">default.production.exception.handler (deprecated)</a></li>
          <tr class="row-odd"><td>window.size.ms (Deprecated. See <a href="datatypes.html#window-serdes">Window Serdes</a> for alternatives.)</td>
          <tr class="row-even"><td>windowed.inner.class.serde (Deprecated. See <a href="datatypes.html#window-serdes">Window Serdes</a> for alternatives.)</td>
            <td>Low</td>
            <td colspan="2">Serde for the inner class of a windowed record. Must implement the <code class="docutils literal"><span class="pre">Serde</span></code> interface.</td>
            <td><code class="docutils literal"><span class="pre">null</span></code></td>
          </tr>
```

**How to spot**
When reviewing docs/streams code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15307: kafka streams configuration docs outdated (#20408).

**Test**
No test added.
