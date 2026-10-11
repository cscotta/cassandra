**Commit**: 3f8e86ab67 · **Ticket**: KAFKA-19013
**Subsystem**: .github/scripts
**Category**: Wrong serialization / deserialization

**Root cause**
Parse the body of a PR and re-format it to a text wrap of 72 characters. Since GitHub will do this automatically when merging PRs with the merge
queue, we should do our best to pre-format the commit message. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before

    if not get_env("GITHUB_ACTIONS"):
        print("This script is intended to by run by GitHub Actions.")
        exit(1)

    logger.debug("```")
    logger.debug("```\n")

// After
import textwrap

def split_paragraphs(text: str):
    """
    Split the given text into a generator of paragraph lines and a boolean "markdown" flag.

    If any line of a paragraph starts with a markdown character, we will assume the whole paragraph
    contains markdown.
    """
    lines = text.splitlines(keepends=True)
    paragraph = []
    markdown = False
```

**How to spot**
When reviewing .github/scripts code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19013 reformat pr body to 72 characters (#19242).

**Test**
No test added.
