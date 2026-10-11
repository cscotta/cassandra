**Commit**: 11904c74e0 · **Ticket**: KAFKA-19166
**Subsystem**: release/git.py
**Category**: Logic error in condition

**Root cause**
The release script was pushing the RC tag off of a temporary branch that
was never merged back into the release branch. This meant that our RC
and release tags were detached from the rest of the repository. This patch changes the release script to merge the RC tag back into the
release branch and pushes both the tag and the branch. Reviewers: Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
def push_tag(tag, remote=push_remote_name, **kwargs):
    cmd("Pushing tag {tag} to {remote}", f"git push {remote} {tag}")
git.push_tag(rc_tag)

// After
def push_ref(ref, remote=push_remote_name, **kwargs):
    cmd(f"Pushing ref {ref} to {remote}", f"git push {remote} {ref}")
def merge_ref(ref, **kwargs):
    __defaults(kwargs)
    cmd(f"Merging ref {ref}", f"git merge {ref}")

git.merge_ref(rc_tag)
git.push_ref(rc_tag)
git.push_ref(starting_branch)
```

**How to spot**
When reviewing release/git.py code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19166: fix rc tag in release script (#19518).

**Test**
No test added.
