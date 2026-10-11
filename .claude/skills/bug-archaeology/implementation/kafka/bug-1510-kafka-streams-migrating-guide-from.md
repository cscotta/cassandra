**Commit**: 85109a5111 · **Ticket**: KAFKA-16339
**Subsystem**: docs/streams
**Category**: Logic error in condition

**Root cause**
Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
                    <li><a class="reference internal" href="#applying-processors-and-transformers-processor-api-integration" id="id24">Applying processors and transformers (Processor API integration)</a></li>
            <div class="section" id="applying-processors-and-transformers-processor-api-integration">
                <span id="streams-developer-guide-dsl-process"></span><h3><a class="toc-backref" href="#id24">Applying processors and transformers (Processor API integration)</a><a class="headerlink" href="#applying-processors-and-transformers-processor-api-integration" title="Permalink to this headline"></a></h3>
                <p>Beyond the aforementioned <a class="reference internal" href="#streams-developer-guide-dsl-transformations-stateless"><span class="std std-ref">stateless</span></a> and
                    <a class="reference internal" href="#streams-developer-guide-dsl-transformations-stateless"><span class="std std-ref">stateful</span></a> transformations, you may also
                    leverage the <a class="reference internal" href="processor-api.html#streams-developer-guide-processor-api"><span class="std std-ref">Processor API</span></a> from the DSL.
                    There are a number of scenarios where this may be helpful:</p>
                <ul class="simple">
                    <li><strong>Customization:</strong> You need to implement special, customized logic that is not or not yet available in the DSL.</li>
                    <li><strong>Combining ease-of-use with full flexibility where it&#8217;s needed:</strong> Even though you generally prefer to use
                        the expressiveness of the DSL, there are certain steps in your processing that require more flexibility and
                        tinkering than the DSL provides.  For example, only the Processor API provides access to a

// After
                    <li><a class="reference internal" href="#applying-processors-processor-api-integration" id="id24">Applying processors (Processor API integration)</a></li>
                    <li><a class="reference internal" href="#transformers-removal-and-migration-to-processors" id="id37">Transformers removal and migration to processors</a></li>
            <div class="section" id="applying-processors-processor-api-integration">
                <a class="headerlink" href="#applying-processors-processor-api-integration" title="Permalink to this headline">
                    <h3>
                        <a class="toc-backref" href="#id24">Applying processors (Processor API integration)</a>
                    </h3>
                </a>
                <p>Beyond the aforementioned <a class="reference internal"
                        href="#streams-developer-guide-dsl-transformations-stateless">
                        <span class="std std-ref">stateless</span></a> and
                    <a class="reference internal" href="#streams-developer-guide-dsl-transformations-stateless"> <span
```

**How to spot**
When reviewing docs/streams code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16339: add kafka streams migrating guide from transform to process (#18314).

**Test**
No test added.
