# Bug 1039: NativeSSTableLoaderClient Uses Driver ClusteringOrder Instead of Internal One

**Commit**: 4db577abf8 · **Ticket**: n/a
**Subsystem**: SSTable Tools / Bulk Loader
**Category**: Wrong constant / default value

---

## Root Cause
`NativeSSTableLoaderClient` uses a wildcard import `com.datastax.driver.core.*` which brings `com.datastax.driver.core.ClusteringOrder` into scope. The file also references `org.apache.cassandra.config.ColumnDefinition.ClusteringOrder` to build `ColumnDefinition` objects, but without an explicit import for the inner class, the Java compiler resolves the unqualified `ClusteringOrder` name to the driver's enum. The driver's `ClusteringOrder` has the same constant names (`ASC`, `DESC`) so the code compiled without errors, but at runtime it could not be used where the Cassandra-internal `ColumnDefinition.ClusteringOrder` was expected, producing class-cast or `NoSuchMethodError` failures during SSTable loading.

The fix adds `import org.apache.cassandra.config.ColumnDefinition.ClusteringOrder;` so the compiler resolves the unqualified name to the correct internal type, shadowing the driver's version.

### Before / After
```java
// BEFORE – only wildcard driver import; compiler picks driver ClusteringOrder
import com.datastax.driver.core.*;
import org.apache.cassandra.config.ColumnDefinition;
// ...
ClusteringOrder order = ClusteringOrder.valueOf(row.getString("clustering_order").toUpperCase());
// resolves to com.datastax.driver.core.ClusteringOrder — wrong type

// AFTER – explicit inner-class import takes precedence
import com.datastax.driver.core.*;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.config.ColumnDefinition.ClusteringOrder;
// ...
ClusteringOrder order = ClusteringOrder.valueOf(...);
// now resolves to org.apache.cassandra.config.ColumnDefinition.ClusteringOrder — correct
```

---

## How to Spot
Whenever a file mixes a third-party wildcard import with internal types that share a simple name, the compiler may silently resolve the name to the wrong type. In review, treat any unqualified type name that also exists in a wildcard-imported package as a red flag and confirm which class the compiler will pick. Prefer explicit imports for internal nested types to avoid ambiguity.

---

## Test
No test added.
