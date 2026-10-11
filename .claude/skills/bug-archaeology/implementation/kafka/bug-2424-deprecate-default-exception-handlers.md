**Commit**: 72e16cb9e1 · **Ticket**: KAFKA-16863
**Subsystem**: docs/streams
**Category**: Wrong serialization / deserialization

**Root cause**
Implements KIP-1056:
 - deprecates default.deserialization.exception.handler in favor of deserialization.exception.handler
 - deprecates default.production.exception.handler in favor of production.exception.handler

Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
              <li><a class="reference internal" href="#default-deserialization-exception-handler" id="id7">default.deserialization.exception.handler</a></li>
              <li><a class="reference internal" href="#default-production-exception-handler" id="id24">default.production.exception.handler</a></li>
          <tr class="row-odd"><td>default.deserialization.exception.handler</td>
          <tr class="row-odd"><td>default.production.exception.handler</td>
          <tr class="row-odd"><td>dsl.store.suppliers.class</td>

// After
              <li><a class="reference internal" href="#deserialization-exception-handler" id="id7">default.deserialization.exception.handler (deprecated since 4.0)</a></li>
              <li><a class="reference internal" href="#production-exception-handler" id="id24">default.production.exception.handler (deprecated since 4.0)</a></li>
              <li><a class="reference internal" href="#deserialization-exception-handler" id="id7">deserialization.exception.handler</a></li>
              <li><a class="reference internal" href="#production-exception-handler" id="id24">production.exception.handler</a></li>
          <tr class="row-odd"><td>default.deserialization.exception.handler (Deprecated. Use deserialization.exception.handler instead.)</td>
          <tr class="row-odd"><td>default.production.exception.handler (Deprecated. Use production.exception.handler instead.)</td>
          <tr class="row-odd"><td>deserialization.exception.handler</td>
            <td>Medium</td>
            <td colspan="2">Exception handling class that implements the <code class="docutils literal"><span class="pre">DeserializationExceptionHandler</span></code> interface.</td>
            <td><code class="docutils literal"><span class="pre">LogAndContinueExceptionHandler</span></code></td>
          </tr>
          <tr class="row-even"><td
```

**How to spot**
When reviewing docs/streams code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16863 : deprecate default exception handlers  (#17005).

**Test**
Test modified in StreamsConfigTest.java covering the corrected behaviour.
