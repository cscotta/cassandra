**Commit**: 81881dee83 · **Ticket**: KAFKA-18760
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
* Deprecate org.apache.kafka.common.Endpoint#listenerName. * Add org.apache.kafka.common.Endpoint#listener to replace
org.apache.kafka.common.Endpoint#listenerName. * Replace org.apache.kafka.network.EndPoint with
org.apache.kafka.common.Endpoint. * Deprecate org.apache.kafka.clients.admin.RaftVoterEndpoint#name
* Add org.apache.kafka.clients.admin.RaftVoterEndpoint#listener to
replace org.apache.kafka.clients.admin.RaftVoterEndpoint#name

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, TaiJuWu
 <tjwu1217@gmail.com>, Jhen-Yung Hsu <jhenyunghsu@gmail.com>, TengYao
 Chi <frankvicky@apache.org>, Ken Huang <s7133700@gmail.com>, Bagda
 Parth  , Kuan-Po Tseng <brandboat@gmail.com>

---------

Sign...

**Before/After**
```java
// Before
                        setName(endpoint.name()).
    private final String name;
     * @param name              The human-readable name for this endpoint. For example, CONTROLLER.
        String name,
        this.name = requireNonNullAllCapsNonEmpty(name);
        return name;
        return name.equals(other.name) &&
        return Objects.hash(name, host, port);
        return name + "://" + hostString + ":" + port;
import org.apache.kafka.common.annotation.InterfaceStability;
@InterfaceStability.Evolving
    private final String listenerName;

// After
                        setName(endpoint.listener()).
    private final String listener;
     * @param listener          The human-readable name for this endpoint. For example, CONTROLLER.
        String listener,
        this.listener = requireNonNullAllCapsNonEmpty(listener);
    /**
     * The listener name for this endpoint.
     */
    public String listener() {
        return listener;
    }

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18760: deprecate optional<string> and return string from public endpoint#listener (#19191).

**Test**
Test modified in TestRaftServer.scala covering the corrected behaviour.
