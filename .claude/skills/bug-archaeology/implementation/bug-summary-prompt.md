# Bug Summary Prompt

Given a bug report, write a single sentence that describes the root cause as a general programming pattern — no class names, method names, variable names, or system-specific terminology. The sentence should be abstract enough that a developer unfamiliar with the codebase would recognize the pattern if they encountered it elsewhere. If the bug is too specific to the system to yield a generalizable pattern, output nothing.

The sentence must satisfy four criteria:
- **Abstract**: replace concrete types and system terms with their structural role (e.g. "wrapper type", "precondition guard", "projected value")
- **Distinctive**: if your draft would equally describe dozens of unrelated bugs, add the mechanical detail that makes this failure unique
- **Concise**: aim for 25 words or fewer; exceed only if the extra words are the only thing that makes the failure distinctive
- **Concrete consequence**: state what actually goes wrong as a direct result — avoid hedging with "can", "may", or "allowing X to happen"

When replacing a domain term with an abstract role, ensure the substitution still preserves the failure mechanism — avoid roles so generic ("entry", "object", "registry") that a reader cannot reconstruct what went wrong.

If a bug has multiple distinct failure modes, summarize the primary cause — the one that makes the others possible — rather than joining both with "and".

## Self-check before outputting

After drafting a sentence, scan every noun and verb for domain-specific vocabulary. Ask: would a developer who has never used this system understand this word? If the answer is no for any word — and replacing it with a structural description would make the sentence unintelligible or too generic — output nothing for that bug instead.

Examples of terms that must be replaced or trigger a skip:
- Storage-engine terms: `SSTable`, `memtable`, `tombstone`, `compaction`, `bloom filter`, `commit log`
- Cluster/protocol terms: `gossip`, `ring`, `token`, `snitch`, `batchlog`, `replication strategy`, `datacenter`
- Query terms: `CQL`, `partition key`, `clustering column`, `materialized view`, `prepared statement`
- Time/version terms: `gcBefore`, `nowInSec`, `ballot`, `epoch` (when used as a Paxos term)

This list is illustrative, not exhaustive — apply the same judgment to any term tied to a specific system's model.

## Examples

**Example 1**

*Input:* `CompressedInputStream` spawned a background `Reader` thread but had no `close()` method and stored no reference to the thread. If the consumer closed early, the background thread blocked indefinitely on a queue put, leaking the thread and the underlying socket.

*Output:* A background thread is spawned but its reference is discarded and no lifecycle method exists to stop it, causing a leak when the consumer closes early.

---

**Example 2**

*Input:* `mutateWithTriggers` accepts a `Collection` and, when trigger mutations need to be appended, casts it to a concrete type and calls `addAll` directly. If the caller passed `Collections.singletonList(...)`, the `addAll` throws `UnsupportedOperationException`.

*Output:* A method calls a mutating operation on a collection parameter directly, failing at runtime when the caller passes an immutable collection.

---

**Example 3** *(not generalizable — output nothing)*

*Input:* A `LIKE '<term>'` query was emitted from the CQL grammar as `Operator.EQ` — the same operator used for non-analyzed full-string equality. The analyzed-index code path then skipped tokenization, so individual tokens did not match.

*Output:* *(none — the root cause is inseparable from this system's specific operator enum and index-mode model)*

---

**Example 4** *(too vague — add mechanical detail)*

*Input:* A guard uses `isNormal()` to protect start/stop operations. A gossip-only member never transitions to `NORMAL`; it stays in `STARTING` permanently. Both `stopGossiping()` and `startGossiping()` throw `IllegalStateException` as a result.

*Too vague — do not output this:* A guard condition fails to account for a legitimate alternative state.

*Output:* A precondition guard checks for a single expected lifecycle state without accounting for a valid operating mode in which the system never enters that state, permanently blocking operations that would otherwise be correct.

---

**Example 5** *(over-abstracted — keep the mechanism visible)*

*Input:* `getColumnDefinitionFromColumnName()` returned any `ColumnDefinition` whose name matched — including `PARTITION_KEY` and `CLUSTERING_COLUMN` definitions. When a Thrift cell name matched a CQL alias, the wrong validator type was used.

*Over-abstracted — do not output this:* A lookup returns entries from a shared registry without filtering by category, causing a name collision to resolve to a metadata-only entry.

*Output:* A lookup by name returns results across all categories without filtering, silently resolving to an entry from the wrong category when names collide.
