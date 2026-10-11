**Commit**: 2c3ce72a05 · **Ticket**: KAFKA-18903
**Subsystem**: committer-tools/update-cache.sh
**Category**: Logic error in condition

**Root cause**
Instead of using the GH CLI (which requires some local setup), we can
just use cURL and JQ. Additionally, the original logic required setting
up alias.update-cache. After the change, this configuration is no longer
necessary. validation on mac:

```
❯ ./update-cache.sh
Local branch 'trunk-cached' successfully updated to f26974b16d (from 19
hours ago).

**Before/After**
```java
// Before
if ! git config --get alias.update-cache > /dev/null; then
  printf '\e[36m%s\n\n  %s\n\e[0m\n' \
    'Hint: you can create a Git alias to execute this script. Example:' \
    "git config alias.update-cache '!bash $(realpath "$0")'"
fi

key="$(
  gh cache list \
    --key 'gradle-home-v1|Linux-X64|test' \
    --sort 'created_at' \
    --limit 1 \
    --json 'key' \

// After
# Get the latest commit SHA that contains the Gradle build cache.
sha=$(curl -s "https://api.github.com/repos/apache/kafka/actions/caches?key=gradle-home-v1&ref=refs/heads/trunk" \
  | jq -r '.actions_caches | max_by(.created_at) | .key | split("-")[4]')
```

**How to spot**
When reviewing committer-tools/update-cache.sh code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18903 update-cache.sh use curl + jq to get sha contains gradle cache instead of gh cli (#19753).

**Test**
No test added.
