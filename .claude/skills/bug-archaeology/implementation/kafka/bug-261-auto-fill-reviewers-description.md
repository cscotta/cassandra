**Commit**: ff64569eca · **Ticket**: KAFKA-20362
**Subsystem**: .github/scripts
**Category**: Logic error in condition

**Root cause**
When a pull request **submit review** event is triggered (comment,
request changes,  or  approval), this change automatically adds the
reviewer to the  Reviewers  trailer in the PR body. Note that **comment** will **NOT** trigger the auto-fill process. This eliminates the need to manually run committer-tools/reviewers.py
before merging. The reviewer's name and email are resolved from their
most recent commit in the repo, falling back to their GitHub profile.

**Before/After**
```java
// Before
from typing import Dict, Optional, TextIO
    cmd = f"gh pr view {pr_number} --json 'title,body,reviews'"

// After
import re
from typing import Dict, List, Optional, TextIO
def resolve_reviewer(login: str) -> tuple:
    """Map a GitHub login to (name, email).

    Tries the repo commit history first, then falls back to the GitHub user profile.
    If the display name is unavailable, the login is used as the name.
    If the email is unavailable, '{login}@email-not-found' is used as a placeholder.
    """
    name = None
    email = None

```

**How to spot**
When reviewing .github/scripts code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20362 auto-fill pr reviewers in pr description (#21928).

**Test**
No test added.
