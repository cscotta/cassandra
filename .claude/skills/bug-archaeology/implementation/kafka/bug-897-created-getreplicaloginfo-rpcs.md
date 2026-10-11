**Commit**: d86ba7f54a · **Ticket**: KAFKA-18681

**Subsystem**: Clients / RPC protocol (GetReplicaLogInfo)

**Category**: Missing feature implementation

**Root cause**
Unclean leader election recovery requires the controller to select the replica with the longest log among all replicas, but there was no RPC to retrieve per-replica log information. Adding `GetReplicaLogInfoRequest`/`GetReplicaLogInfoResponse` (API key 93) provides a standard broker-to-broker (or controller-to-broker) mechanism to query a replica's log end offset and epoch, enabling the controller to make an informed selection during unclean recovery rather than picking arbitrarily.

**Before/After**
```java
// Before: GET_REPLICA_LOG_INFO absent from ApiKeys enum
DELETE_SHARE_GROUP_OFFSETS(ApiMessageType.DELETE_SHARE_GROUP_OFFSETS);

// After: new API key added
DELETE_SHARE_GROUP_OFFSETS(ApiMessageType.DELETE_SHARE_GROUP_OFFSETS),
GET_REPLICA_LOG_INFO(ApiMessageType.GET_REPLICA_LOG_INFO);

// Request/Response parse cases added in AbstractRequest/AbstractResponse
case GET_REPLICA_LOG_INFO:
    return GetReplicaLogInfoRequest.parse(readable, apiVersion);
```

**How to spot**
When a new recovery or diagnostic workflow requires information that is not available via existing RPCs, a new API key must be defined in `ApiKeys` and corresponding request/response schema files must be added; missing these causes `UnknownServerException` at runtime.

**Test**
`RequestResponseTest` verifies round-trip serialization/deserialization of `GetReplicaLogInfoRequest` and `GetReplicaLogInfoResponse` at all supported versions.
