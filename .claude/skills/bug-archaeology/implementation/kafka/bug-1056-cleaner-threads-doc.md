## Commit

af11146bc9 — MINOR: Document lower bound of log.cleaner.threads after log.cleaner.enable removal

## Ticket

MINOR (upgrade documentation)

## Subsystem

Documentation / log cleaner configuration

## Category

Wrong constant / default value

## Root cause

The upgrade notes for the removal of `log.cleaner.enable` did not mention that `log.cleaner.threads` would gain a lower bound of 1 once the enable flag was gone. Previously a user could set `log.cleaner.threads=0` to effectively disable the cleaner. After `log.cleaner.enable` is removed, the cleaner is always active and a value of 0 for thread count is no longer meaningful, but the documentation gave no warning about this change in accepted range.

## Before / After

```html
<!-- Before — no mention of thread count lower bound -->
<li>log.cleaner.enable has been removed.</li>

<!-- After — lower-bound caveat added -->
<li>log.cleaner.enable has been removed. Note that log.cleaner.threads
    will have a lower bound of 1 after this change.</li>
```

## How to spot

When a feature-enable flag is removed and the previously guarded feature is now always on, audit all configuration parameters that interacted with that flag for range or default changes that should be documented in upgrade notes.

## Test

Documentation-only change; no automated test added.
