**Commit**: 7b5d4caced · **Ticket**: KAFKA-20362
**Subsystem**: .github/scripts
**Category**: Resource leak

**Root cause**
Fix https://github.com/apache/kafka/pull/21928#issuecomment-4284041251. When a reviewer has "Keep my email addresses private" enabled, their
real email is unreachable through either the commits API (GitHub
rewrites the author email to the `@users.noreply.github.com` form on
squash-merge) or the GitHub user profile (which typically returns null). The previous script wrote that noreply email, or a
`{login}@email-not-found` placeholder when both tiers missed, into the
`Reviewers:` trailer -- neither identifies the reviewer. This PR adds two fallbacks, in order:

1.

**Before/After**
```java
// Before
    Tries the repo commit history first, then falls back to the GitHub user profile.
    If the display name is unavailable, the login is used as the name.
    If the email is unavailable, '{login}@email-not-found' is used as a placeholder.
    # Tier 1: find from repo commit history
                email = author.get("email")
    # Tier 2: GitHub user profile
                    email = user.get("email")

// After
    Tries three tiers in order: repo commit history, GitHub user profile,
    and past `Reviewers:` trailers in git log (matched by name).
    Noreply emails (@users.noreply.github.com) are treated as missing since
    they are GitHub privacy placeholders that do not identify the reviewer.
    Returns (name, None) when no usable email is found; the caller falls
    back to the '(@login)' form in the Reviewers trailer.
    def _usable_email(e):
        if not e or e.endswith("@users.noreply.github.com"):
            return None
        return e

    # Tier 1: find from repo commit history. Misses when the reviewer has no
```

**How to spot**
When reviewing .github/scripts code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20362 fall back to github handle when reviewer email is unavailable (#22108).

**Test**
No test added.
