**Commit**: c7d574e75f · **Ticket**: KAFKA-20371
**Subsystem**: connect/runtime
**Category**: Logic error in condition

**Root cause**
The new constructor will enable us to use `List.of()` instead of
`List.of().iterator()`. This new constructor will simply extract the
iterator and pass it to the parent class. Reviewers: Christo Lolov <lolovc@amazon.com>, Chia-Ping Tsai
<chia7712@gmail.com>

---------

Signed-off-by: PoAn Yang <payang@apache.org>

**Before/After**
```java
// Before
                    setPort(9092)).iterator())));
                        ).iterator()))
                ).collect(Collectors.toList()).iterator()))
            .setTopics(new CreatableTopicResultCollection(Arrays.stream(topics).iterator()));
            .setResponses(new DeletableTopicResultCollection(Arrays.stream(topics).iterator()));
            .setNodes(new DescribeQuorumResponseData.NodeCollection(Collections.singleton(new DescribeQuorumResponseData.Node().setNodeId(1)).iterator())));
                                    .setLowWatermark(3)).iterator()))).iterator()))), nodes.get(0));
                    ).iterator())));
                            ).iterator())),
                            ).iterator()))
                    ).collect(Collectors.toList()).iterator()))
                                ).iterator()))

// After
                    setPort(9092)))));
                        )))
                ).collect(Collectors.toList())))
            .setTopics(new CreatableTopicResultCollection(Arrays.asList(topics)));
            .setResponses(new DeletableTopicResultCollection(Arrays.asList(topics)));
            .setNodes(new DescribeQuorumResponseData.NodeCollection(Collections.singleton(new DescribeQuorumResponseData.Node().setNodeId(1)))));
                                    .setLowWatermark(3)))))))), nodes.get(0));
                    ))));
                            ))),
                            )))
                    ).collect(Collectors.toList())))
```

**How to spot**
When reviewing connect/runtime code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20371: add iterable constructor to generated message collections (#21894).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
