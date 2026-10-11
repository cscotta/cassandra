**Commit**: 05696037d3 · **Ticket**: KAFKA-12823
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Implements KIP-1087

Reviewers: Matthias J. Sax <matthias@confluent.io>, Lucas Brutschy <lbrutschy@confluent.io>, Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before

            output, and intermediate topics) and <a class="reference internal" href="manage-topics.html#streams-developer-guide-topics-internal"><span class="std std-ref">internal topics</span></a> differently
            <li>Intermediate topics: Skip to the end of the topic, i.e., set the application&#8217;s committed consumer offsets for all partitions to each partition&#8217;s <code class="docutils literal"><span class="pre">logSize</span></code> (for consumer group <code class="docutils literal"><span class="pre">application.id</span></code>).</li>
                <li><p class="first">You should manually delete and re-create any intermediate topics before running the application reset tool. This will free up disk space in Kafka brokers.</p>
                </li>
                <li><p class="first">You should delete and recreate intermediate topics before running the application reset tool, unless the following applies:</p>
                    <blockquote>
                        <div><ul class="simple">
                            <li>You have external downstream consumers for the application&#8217;s intermediate topics.</li>
                            <li>You are in a development environment where manually deleting and re-creating intermediate topics is unnecessary.</li>
                        </ul>
                        </div></blockquote>

// After
            and output) and <a class="reference internal" href="manage-topics.html#streams-developer-guide-topics-internal"><span class="std std-ref">internal topics</span></a> differently
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12823 remove deprecated method kstream#through (#16761).

**Test**
Test modified in StreamsBuilderTest.java covering the corrected behaviour.
