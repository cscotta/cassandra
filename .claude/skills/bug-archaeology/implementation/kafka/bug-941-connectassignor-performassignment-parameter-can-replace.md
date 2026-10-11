**Commit**: 15ad3016b7 · **Ticket**: KAFKA-19140
**Subsystem**: connect/runtime
**Category**: Logic error in condition

**Root cause**
The protocol type; for Connect assignors this is "eager", "compatible",
or "sessioned"

Since `ConnectAssignor` is an interface and the protocol parameter is
restricted to "eager", "compatible", or "sessioned", it aligns with the
existing ConnectProtocolCompatibility enum. Therefore, we can update the
code to use `ConnectProtocolCompatibility` directly. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
     * @param protocol the protocol type; for Connect assignors this is "eager", "compatible", or "sessioned"
    Map<String, ByteBuffer> performAssignment(String leaderId, String protocol,
    public Map<String, ByteBuffer> performAssignment(String leaderId, String protocol,
    public Map<String, ByteBuffer> performAssignment(String leaderId, String protocol,
        short protocolVersion = ConnectProtocolCompatibility.fromProtocol(protocol).protocolVersion();
        return ConnectProtocolCompatibility.fromProtocol(protocol) == EAGER
               ? eagerAssignor.performAssignment(leaderId, protocol, allMemberMetadata, this)
               : incrementalAssignor.performAssignment(leaderId, protocol, allMemberMetadata, this);

// After
     * @param protocol the protocol type
    Map<String, ByteBuffer> performAssignment(String leaderId, ConnectProtocolCompatibility protocol,
    public Map<String, ByteBuffer> performAssignment(String leaderId, ConnectProtocolCompatibility protocol,
    public Map<String, ByteBuffer> performAssignment(String leaderId, ConnectProtocolCompatibility protocol,
        short protocolVersion = protocol.protocolVersion();
        ConnectProtocolCompatibility protocolCompatibility = ConnectProtocolCompatibility.fromProtocol(protocol);
        return protocolCompatibility == EAGER
               ? eagerAssignor.performAssignment(leaderId, protocolCompatibility, allMemberMetadata, this)
               : incrementalAssignor.performAssignment(leade
```

**How to spot**
When reviewing connect/runtime code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19140 connectassignor#performassignment parameter can be replace to connectprotocolcompatibility (#19476).

**Test**
Test modified in IncrementalCooperativeAssignorTest.java covering the corrected behaviour.
