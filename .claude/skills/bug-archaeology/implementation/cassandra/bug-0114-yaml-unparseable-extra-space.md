# Bug 0114: Extra Space Before Colon Makes Default cassandra.yaml Unparseable When Uncommented

**Commit**: 027689ae48 · **Ticket**: CASSANDRA-20359
**Subsystem**: `conf/cassandra.yaml`, `conf/cassandra_latest.yaml`
**Category**: Wrong constant / default value

---

## Root Cause

Several commented-out YAML stanzas in the default configuration files used `key : value` (with a space before the colon) instead of the standards-compliant `key: value` form. While the Cassandra YAML parser happens to tolerate the extra space, external tooling (config validators, schema generators, downstream automation) that strictly follows the YAML specification would fail to parse the file as soon as those lines were uncommented. The fix removes the stray space in six key-value pairs across both `cassandra.yaml` and `cassandra_latest.yaml`.

### Before / After

```yaml
# BEFORE
#  class_name : org.apache.cassandra.auth.MutualTlsAuthenticator
#  parameters :
```

```yaml
# AFTER
#  class_name: org.apache.cassandra.auth.MutualTlsAuthenticator
#  parameters:
```

---

## How to Spot

Configuration file templates that contain commented-out blocks are not exercised by normal test suites, so formatting errors accumulate silently. When reviewing changes to default config files, run the file through a strict YAML linter (`python -c "import yaml; yaml.safe_load(open('cassandra.yaml'))"`) to catch syntax violations before they land. Pay particular attention to lines whose comments are intended to be uncommented by users; those lines must be parseable in isolation.

---

## Test

No automated test added; the fix is a pure whitespace correction to static configuration files.
