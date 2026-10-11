**Commit**: d0f4d691b5 · **Ticket**: KAFKA-17322
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Josep Prat <josep.prat@aiven.io>

**Before/After**
```java
// Before
// (no removals)

// After

                    b.append("<p><b>Request header version:</b> ");
                    b.append(key.requestHeaderVersion((short) i));
                    b.append("</p>\n");


                    b.append("<p><b>Response header version:</b> ");
                    b.append(key.responseHeaderVersion((short) i));
                    b.append("</p>\n");

            <li><a href="#protocol_headers">Request and Response Headers</a>
<h5 class="anchor-heading"><a id="protocol_headers" class="anchor-link"></a><a href="#protocol_headers">Request and Response Headers</a></h5>
```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17322: document required header versions in protocol guide (#16867).

**Test**
No test added.
