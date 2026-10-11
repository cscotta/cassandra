**Commit**: 3066019efa · **Ticket**: KAFKA-16521
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Colin P. McCabe <cmccabe@apache.org>

**Before/After**
```java
// Before
        return "RaftVoterEndpoint" +
            "(name=" + name +
            ", host=" + host +
            ", port=" + port +
            ")";
    // CurrentVoters: [{"id": 0, "directoryId": "UUID1", "endpoints": [{"name": "C", "securityProtocol": "SSL", "host": "controller-0", "port": 1234}]}, {"id": 1, ... }]}]
            sb.append("\"directoryId\": ").append("\"").append(directoryId.equals(Uuid.ZERO_UUID) ? "null" : directoryId).append("\"");
                sb.append(", \"endpoints\": ");
                    sb.append(endpoint.toString()).append(", ");
            outputs[6].matches("CurrentVoters:\\s+\\[\\{\"id\":\\s+\\d+,\\s+\"directoryId\":\\s+\\S+,\\s+\"endpoints\":\\s+.*}]"),

// After
        // enclose IPv6 hosts in square brackets for readability
        String hostString = host.contains(":") ? "[" + host + "]" : host;
        return name + "://" + hostString + ":" + port;
    // CurrentVoters: [{"id": 0, "directoryId": "UUID1", "endpoints": ["C://controller-0:1234"]}]
            sb.append("\"directoryId\": ").append(directoryId.equals(Uuid.ZERO_UUID) ? "null" : "\"" + directoryId + "\"");
                sb.append(", \"endpoints\": [");
                    sb.append("\"");
                    sb.append(endpoint.toString()).append("\", ");
                sb.append("]");
            outputs[6].matches("CurrentVoters:\\s+\\[\\{\"id\":\\s+\\d+,\\s+\"directoryId\":\\s+\\S+,\\s+" +
                "\"endpoints\":\\s+\\[\"\\S+://\\[?\\S+]?:\\d+\",?.*]"),
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16521: have raft endpoints printed as name://host:port (#16830).

**Test**
Test modified in MetadataQuorumCommandTest.java covering the corrected behaviour.
