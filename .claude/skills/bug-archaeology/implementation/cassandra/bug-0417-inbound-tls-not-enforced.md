# Bug 0417: Inbound TLS Not Enforced — Unencrypted Connections Accepted When Encryption Required

**Commit**: c2f24d2c45 · **Ticket**: n/a
**Subsystem**: net (internode messaging / streaming)
**Category**: Logic error in condition

---

## Root Cause

The encryption check (`isEncryptedChannel`) was performed only on the outbound side in `OutboundTcpConnectionPool`.  `IncomingTcpConnection` and `IncomingStreamingConnection` never verified that an incoming socket was actually TLS-wrapped before processing the handshake.  A remote node configured without TLS could therefore connect to a node that had `internode_encryption = dc` (or `all`) and exchange messages in plaintext; the receiving node would not reject or even log the unencrypted connection.  Additionally, the encryption-decision logic was duplicated only in `OutboundTcpConnectionPool.isEncryptedChannel()`; the fix consolidated it into `EncryptionOptions.ServerEncryptionOptions.shouldEncrypt()` and called it from both inbound connection handlers.

### Before / After

```java
// BEFORE – IncomingTcpConnection: no check at all after reading peer address
from = CompactEndpointSerializationHelper.deserialize(in);
// continues processing with no TLS verification
```

```java
// AFTER – reject unencrypted connections when encryption is required
from = CompactEndpointSerializationHelper.deserialize(in);
if (isEncryptionRequired(from) && !isEncrypted())
{
    logger.warn("Peer {} attempted to establish an unencrypted connection ...", ...);
    throw new IOException("Peer " + from + " attempted an unencrypted connection");
}
```

---

## How to Spot

Security-enforcement code is suspicious whenever a policy is checked on only one side of a connection (outbound) but not the other (inbound).  Review any `Incoming*Connection` class that reads a peer identifier from the wire: after the identity is known, the server should immediately verify that the channel's transport properties (TLS, compression) match what the policy requires for that peer.

---

## Test

New `InternodeEncryptionEnforcementTest` distributed test creates a two-node cluster where only one node has `internode_encryption=dc` configured and verifies that no unencrypted connections are established across DC boundaries.
