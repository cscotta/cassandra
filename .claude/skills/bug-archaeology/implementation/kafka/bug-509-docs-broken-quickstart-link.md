## Bug 509 — Docs: broken quickstart link on Kafka Connect user-guide page

**Commit:** 6b5a2e470b
**Ticket:** KAFKA-20027
**Subsystem:** docs
**Category:** Logic error in condition

### Root cause
Two documentation pages contained broken relative links to the quickstart guide after a docs restructuring that moved pages into subdirectories. `docs/kafka-connect/user-guide.md` linked to `../quickstart` (which resolved to `docs/quickstart`, a path that no longer exists) and `docs/getting-started/quickstart.md` linked to `/documentation/#connectconfigs_plugin.path` (an old anchor format). Both links produced 404s in the rendered site.

### Before
```markdown
<!-- user-guide.md -->
The [quickstart](../quickstart) provides a brief example …
<!-- quickstart.md -->
See [plugin.path](/documentation/#connectconfigs_plugin.path)
```

### After
```markdown
<!-- user-guide.md -->
The [quickstart](../getting-started/quickstart) provides a brief example …
<!-- quickstart.md -->
See [plugin.path](../../configuration/kafka-connect-configs/#connectconfigs_plugin.path)
```

### How to spot
After any docs directory restructuring, relative links in Markdown that use `../` paths without being updated will silently 404. Running a link checker (e.g., `lychee` or a CI docs build) catches these.

### Test
Documentation-only change; verified by rendered site link check.
