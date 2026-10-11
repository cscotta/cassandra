# Bug 1552: Fix Word Count — Replace describe_ring with CQL Peers Query

**Commit**: 337e4a8292 · **Ticket**: CASSANDRA-7200
**Subsystem**: examples/hadoop_word_count, examples/hadoop_cql3_word_count
**Category**: Logic error in condition

---

## Root Cause

The `WordCountSetup` examples used `client.describe_ring(keyspace)` to count the number of cluster nodes, which returns the number of token ranges rather than the number of hosts — these differ dramatically when vnodes are enabled. The magnitude value was then used to calculate a sleep duration proportional to cluster size, meaning the sleep could be wildly inflated with vnodes. The fix replaces the ring-size call with a direct count of rows in `system.peers`, which accurately reflects the number of remote hosts.

### Before / After

```java
// BEFORE
int magnitude = client.describe_ring(WordCount.KEYSPACE).size();
```

```java
// AFTER
int magnitude = getNumberOfHosts(client);
// getNumberOfHosts queries system.peers via get_range_slices
List<KeySlice> ls = client.get_range_slices(parent, predicate, keyrRange, ConsistencyLevel.ONE);
return ls.size();
```

---

## How to Spot

When computing a count that represents nodes/hosts, verify the source is node-based (e.g., `system.peers`) rather than token-based (`describe_ring`). With vnodes, `describe_ring` returns O(num_tokens × nodes) entries, not O(nodes).

---

## Test

No test added.
