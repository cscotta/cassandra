**Commit**: d0e516a872 · **Ticket**: KAFKA-18803
**Subsystem**: metadata / image / node / MetadataImageNode
**Category**: Wrong constant / default value

**Root cause**
`MetadataImageNode.CHILDREN` mapped the `"acls"` key to a factory that creates an `AclsImageByIdNode` (which presents ACLs as a flat id→acl map), but the intended node for the ACL branch in the metadata shell tree is `AclsImageNode` (which presents ACLs grouped by resource type). The mismatch caused ACL entries to appear at the wrong level when browsing the metadata shell. `AclsImage.toString()` was similarly broken because it also used `AclsImageByIdNode` instead of `AclsImageNode`.

**Before/After**
```java
// Before — wrong node type for the acls branch
AclsImageNode.NAME, image -> new AclsImageByIdNode(image.acls()),

// After — correct node type
AclsImageNode.NAME, image -> new AclsImageNode(image.acls()),
```

**How to spot**
When a registry or map associates a string key with a factory lambda, verify that the returned type actually matches the key's semantics. Here the key was `AclsImageNode.NAME` but the value produced a different (less structured) node class.

**Test**
`MetadataImageNodeTest.shouldGetAclsImageNode` — creates a `MetadataImageNode`, calls `child(AclsImageNode.NAME)`, and asserts the result is an instance of `AclsImageNode` (not `AclsImageByIdNode`).
