# Bug 0466: EC2Snitch Legacy Mode Detection Incorrectly Rejects Valid Mixed-Format DC Names

**Commit**: 6fc8920889 · **Ticket**: CASSANDRA-15878
**Subsystem**: `src/java/org/apache/cassandra/locator/Ec2Snitch.java`
**Category**: Logic error in condition

---

## Root Cause

The old validation logic used a bidirectional check: `dcUsesLegacyFormat != usingLegacyNaming`. This made the validator reject a cluster that was in legacy mode whenever the observed DC name happened to also match the standard naming pattern (e.g., `us-west-2` is valid in both the old and new naming schemes). In such regions, a pre-4.0 cluster upgrading in legacy mode would be falsely flagged as a mixed-mode mismatch and blocked from starting. The fix narrows the legacy-detection regex to only flag names that are unambiguously legacy (no trailing digit, e.g. `us-east`), and changes the condition to `dcUsesLegacyFormat && !usingLegacyNaming` so that ambiguous names never cause a false rejection.

### Before / After

```java
// BEFORE
boolean dcUsesLegacyFormat = !dc.matches("[a-z]+-[a-z].+-[\\d].*");
if (dcUsesLegacyFormat != usingLegacyNaming)
    valid = false;
```

```java
// AFTER
boolean dcUsesLegacyFormat = dc.matches("^[a-z]+-[a-z]+$");
if (dcUsesLegacyFormat && !usingLegacyNaming)
    valid = false;
```

---

## How to Spot

Bidirectional regex-based mode checks (`a != b`) are dangerous when the two modes have overlapping name spaces. Whenever a region/datacenter name can be valid in both old and new conventions, the detection heuristic must be asymmetric: only flag names that are exclusively in the old convention, and rely on an unambiguous signal (rack name format) as the authoritative differentiator for everything else.

---

## Test

Two new unit tests added: `validate_RequiresLegacy_DCValidStandardAndLegacy` confirms that `us-west-2` with legacy-style racks passes validation in legacy mode; `validate_RequiresLegacy_RackInvalidForLegacy` confirms that a standard-style rack name (`us-west-2a`) is enough to detect a mismatch.
