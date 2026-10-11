**Commit**: 794e9a4a52 · **Ticket**: KAFKA-14995
**Subsystem**: committer-tools/README.md
**Category**: Logic error in condition

**Root cause**
Add a Python script that analyzes our Git history to find top contributors. This can be used by committers to update
the list of contributors in .asf.yaml without a lot of tedious effort. Co-authored-by: stevenbooke <steviebeee55@gmail.com>
Co-authored-by: Joao Pedro Fonseca <fonsdant@gmail.com>
Reviewers: David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
# Refresh Collaborators Script

The Refresh Collaborators script automates the process of fetching contributor
data from GitHub repositories, filtering top contributors who are not part of
the existing committers, and updating a local configuration file (.asf.yaml) to
include these new contributors.

## Table of Contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
```

**How to spot**
When reviewing committer-tools/README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14995: automate asf.yaml collaborators refresh (#17124).

**Test**
No test added.
