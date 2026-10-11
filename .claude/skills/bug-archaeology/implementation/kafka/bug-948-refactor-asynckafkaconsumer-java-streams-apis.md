**Commit**: adcf10ca8b · **Ticket**: KAFKA-19297
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Profiling has shown that using the Collections Streams API approach adds
unnecessary overhead compared to a traditional for loop. Minor revisions
to the code have been made to use simpler constructs to improve
performance. Reviewers: Lianet Magrans <lmagrans@confluent.io>, Andrew Schofield
 <aschofield@confluent.io>

**Before/After**
```java
// Before
import java.util.stream.Collectors;
                List<Node> leaderNodes = response.data().nodeEndpoints().stream()
                    .map(e -> new Node(e.nodeId(), e.host(), e.port(), e.rack()))
                    .filter(e -> !e.equals(Node.noNode()))
                    .collect(Collectors.toList());
        return fetchable.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().build()));
        return fetchable.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().build()));
import java.util.stream.Collectors;
        log.info("Revoking previously assigned partitions {}", revokedPartitions.stream().map(TopicPartition::toString).collect(Collectors.joining(", ")));
import java.util.stream.Collectors;

// After
import java.util.ArrayList;
                List<Node> leaderNodes = new ArrayList<>();

                for (FetchResponseData.NodeEndpoint e : response.data().nodeEndpoints()) {
                    Node node = new Node(e.nodeId(), e.host(), e.port(), e.rack());

                    if (!node.equals(Node.noNode()))
                        leaderNodes.add(node);
                }

        return convert(fetchable);
        return convert(fetchable);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19297: refactor asynckafkaconsumer's use of java streams apis in critical sections (#19917).

**Test**
No test added.
