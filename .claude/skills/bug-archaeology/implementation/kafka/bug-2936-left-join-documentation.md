**Commit**: 7763243752 · **Ticket**: KAFKA-12317
**Subsystem**: docs/streams
**Category**: Logic error in condition

**Root cause**
FK left-join was changed via KIP-962. This PR updates the docs accordingly. Reviewers: Ayoub Omari <ayoubomari1@outlook.fr>, Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
                                        <li>
                                            Records for which the <code class="docutils literal"><span class="pre">foreignKeyExtractor</span></code> produces <code class="docutils literal"><span class="pre">null</span></code> are ignored and do not trigger a join.
                                            If you want to join with <code class="docutils literal"><span class="pre">null</span></code> foreign keys, use a suitable sentinel value to do so (i.e. <code class="docutils literal"><span class="pre">"NULL"</span></code> for a String field, or <code class="docutils literal"><span class="pre">-1</span></code> for an auto-incrementing integer field).
                                        </li>

// After
                                        <li>
                                            Records for which the <code class="docutils literal"><span class="pre">foreignKeyExtractor</span></code> produces <code class="docutils literal"><span class="pre">null</span></code> are ignored and do not trigger a join.
                                            If you want to join with <code class="docutils literal"><span class="pre">null</span></code> foreign keys, use a suitable sentinel value to do so (i.e. <code class="docutils literal"><span class="pre">"NULL"</span></code> for a String field, or <code class="docutils literal"><span class="pre">-1</span></code> for an auto-incrementing integer field).
                                        </li>
```

**How to spot**
When reviewing docs/streams code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12317: update fk-left-join documentation (#15689).

**Test**
No test added.
