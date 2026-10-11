**Commit**: 54a83c5b9a · **Ticket**: KAFKA-17839
**Subsystem**: tools/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                     ConsumerGroupCommand.ConsumerGroupService service = consumerGroupService(new String[]{"--bootstrap-server", clusterInstance.bootstrapServers(), "--describe", "--group", protocolGroup})
                        return res.getKey().trim().split("\n").length == 2 && res.getValue().isEmpty();
                            return (numLines == expectedNumLines) && res.getValue().isEmpty();
                    try (ConsumerGroupCommand.ConsumerGroupService service = consumerGroupService(new String[]{"--bootstrap-server", clusterInstance.bootstrapServers(), "--describe", "--all-groups"})) {
                            return (numLines == expectedNumLines) && res.getValue().isEmpty();
                     ConsumerGroupCommand.ConsumerGroupService service = consumerGroupService(new String[]{"--bootstrap-server", clusterInstance.bootstrapServers(), "--describe", "--group", group})

// After
import java.util.concurrent.atomic.AtomicBoolean;
                List<String> cgcArgs = new ArrayList<>(List.of("--bootstrap-server", clusterInstance.bootstrapServers(), "--describe", "--group", protocolGroup));
                cgcArgs.addAll(describeType);
                     ConsumerGroupCommand.ConsumerGroupService service = consumerGroupService(cgcArgs.toArray(new String[0]))
                        return res.getKey().trim().split("\n").length == 2 && res.getValue().isEmpty() && checkArgsOutput(cgcArgs, res.getKey().trim().split("\n")[0]);
                            return (numLines == expectedNumLines) && res.getValue().isEmpty() && checkArgsOutput(cgcArgs, res.getKey().trim().split("\n")[0]);
                    List<String> cgcArgs = new ArrayList<>(List.of("--bootstrap-server", clusterInstance.bootstrapServers(), "--describe", "--all-groups"));
                    cgcArgs.addAll(describeType);
                    try (ConsumerGroupCommand.ConsumerGroupService service = consumerGroupService(cgcArgs.toArray(new String[0]))) {
                            return (numLines == expectedNumLines) && res.getValue().isEmpty() && checkArgsOutput(cgcArgs, res.getKey().trim().split("\n")[0]);
                List<String> cgcArgs = new ArrayList<>(List.of("--bootstrap-server", clusterInstance.bootstrapServers(), "--describe", "--group", group));
                cgcArgs.addAll(describeType);
```

**How to spot**
When reviewing tools/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17839 describeconsumergrouptest doesn't really test different arguments (#17556).

**Test**
Test modified in DescribeConsumerGroupTest.java covering the corrected behaviour.
