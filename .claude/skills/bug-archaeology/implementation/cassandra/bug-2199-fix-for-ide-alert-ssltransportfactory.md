# Bug 2199: SSLTransportFactory in Wrong Package Causes Unresolved Import

**Commit**: f593717ace · **Ticket**: CASSANDRA-4239
**Subsystem**: tools/stress/src/org/apache/cassandra/stress/SSLTransportFactory.java
**Category**: Logic error in condition

---

## Root Cause

`SSLTransportFactory` was placed in the package `org.apache.cassandra.cli.transport` even though it lived under the `tools/stress` source tree and depended on `org.apache.cassandra.stress.Session` and `org.apache.cassandra.stress.Stress`. The mismatched package declaration caused IDE and compiler warnings (unresolved cross-package references) and meant users had to specify the wrong fully-qualified class name when passing `--transport-factory` to the stress tool. The file was moved to the `org.apache.cassandra.stress` package where it belongs, unused imports of `Session` and `Stress` were removed, and the help text was updated to reference the correct class name.

### Before / After

```java
// BEFORE – wrong package, unused imports
package org.apache.cassandra.cli.transport;
import org.apache.cassandra.stress.Session;
import org.apache.cassandra.stress.Stress;
```

```java
// AFTER – correct package, unused imports removed
package org.apache.cassandra.stress;
// Session and Stress imports removed
```

---

## How to Spot

Source files whose declared package does not match their location in the directory tree will generate IDE alerts and can introduce subtle class-not-found failures at runtime when users copy the wrong fully-qualified name from documentation. Enforce that directory structure and `package` declarations always agree.

---

## Test

No test added.
