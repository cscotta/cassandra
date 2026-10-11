# Bug 0500: nodetool describering Missing Leading Comma Without --with-ports

**Commit**: 007a9357cf · **Ticket**: CASSANDRA-15682
**Subsystem**: `src/java/org/apache/cassandra/service/TokenRange.java`
**Category**: Logic error in condition

---

## Root Cause

`TokenRange#toString(boolean withPorts)` used an `if/else` branch to decide whether to prepend a comma before the `endpoints:`, `rpc_endpoints:`, and `endpoint_details:` fields. In the `withPorts` branch it added `", endpoints:"` (with leading comma), but in the non-ports `else` branch it omitted the comma and wrote `"endpoints:"` directly. The field immediately followed the closing parenthesis of the token-range section, producing malformed output like `(start_token:X, end_token:Yendpoints:[...])` when ports were disabled, which was the default case.

### Before / After

```java
// BEFORE — non-ports branch omits the leading comma and separator
if (withPorts) {
    sb.append(", endpoints:").append(hosts);
    sb.append(", rpc_endpoints:").append(rpcs);
    sb.append(", endpoint_details:").append(endpointDetails);
} else {
    sb.append("endpoints:").append(hosts);       // missing ", "
    sb.append("rpc_endpoints:").append(rpcs);    // missing ", "
    sb.append("endpoint_details:").append(endpointDetails);
}
```

```java
// AFTER — always use the comma-prefixed form; withPorts affects only how addresses are formatted
sb.append(", endpoints:").append(hosts);
sb.append(", rpc_endpoints:").append(rpcs);
sb.append(", endpoint_details:").append(endpointDetails);
```

---

## How to Spot

When an `if/else` branch differs only in string literals that should be structurally identical, that is a strong signal that one branch was accidentally edited. Reviewers should verify that formatting helpers produce structurally equivalent output across all code paths, and prefer extracting shared formatting logic to eliminate the duplication that makes this class of divergence possible.

---

## Test

No test added.
