**Commit**: d964574ab9 · **Ticket**: KAFKA-18942
**Subsystem**: committer-tools/README.md
**Category**: Logic error in condition

**Root cause**
Allow `reviewers.py` to set the "Reviewers:" line in a PR body. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
## Find Reviewers
Git trailer. It parses the Git log to gather a set of "Authors" and "Reviewers".
Some simple string prefix matching is done to find candidates.
        out = "\n\nReviewers: "
        out += ", ".join([f"{name} <{email}>" for name, email, _ in selected_reviewers])
        out += "\n"
        print(out)


// After
## Find Reviewers and Update to PR body
Git trailer to PR body. It parses the Git log to gather a set of "Authors" and "Reviewers".
Some simple string prefix matching is done to find candidates.
After entering the pull request number, the script updates the "Reviewers:" trailer accordingly.
If the PR body already contains a "Reviewers:" trailer, the script replaces it with the updated list of reviewers.
import json
import shlex
import subprocess
import tempfile
def update_trailers(body, trailer):
    with tempfile.NamedTemporaryFile() as fp:
        fp.write(body.encode())
```

**How to spot**
When reviewing committer-tools/README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18942: add reviewers to pr body with committer-tools (#19168).

**Test**
No test added.
