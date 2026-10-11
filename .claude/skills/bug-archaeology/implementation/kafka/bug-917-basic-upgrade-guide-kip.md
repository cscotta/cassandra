**Commit**: 53d654ab6e · **Ticket**: KAFKA-19379
**Subsystem**: docs/streams
**Category**: Logic error in condition

**Root cause**
Basic documentation describing:   - That it's in EA now

 - What it does

 - What features are not yet supported

 - How to enable it / disable it

 - Any changes in the interfaces

          - kafka-streams-groups.sh

          - StreamsGroupDescribe

 - How to provide feedback

Reviewers: Andrew Schofield <aschofield@confluent.io>, Matthias J. Sax
 <matthias@confluent.io>

---------

Co-authored-by: Copilot <175728472+Copilot@users.noreply.github.com>
Co-authored-by: Matthias J. Sax <mjsax@apache.org>

**Before/After**
```java
// Before
// (no removals)

// After
              <li><a class="reference internal" href="#group-protocol" id="id47">group.protocol</a></li>
        <div class="section" id="group-protocol">
          <h4><a class="toc-backref" href="#id47">group.protocol</a><a class="headerlink" href="#group-protocol" title="Permalink to this headline"></a></h4>
          <blockquote>
            <div>
              <p>
                The group protocol used by the Kafka Streams client used for coordination.
                It determines how the client will communicate with the Kafka brokers and other clients in the same group.
                The default value is <code class="docutils literal"><span class="pre">"classic"</span></code>, which is the classic consumer group protocol.
                Can be set to <code class="docutils literal"><span class="pre">"streams"</span></code> (requires broker-side enablement) to enable the new Kafka Streams group protocol.
                Note that the "streams" rebalance protocol is an Early Access feature and should not be used in production.
              </p>
```

**How to spot**
When reviewing docs/streams code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19379: basic upgrade guide for kip-1071 ea (#20029).

**Test**
No test added.
