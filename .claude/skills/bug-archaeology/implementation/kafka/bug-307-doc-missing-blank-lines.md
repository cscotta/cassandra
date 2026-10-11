## Commit
ccd8330e9e

## Ticket
KAFKA-20355

## Subsystem
Documentation

## Category
Logic error in condition

## Root cause
`docs/configuration/system-properties.md` was missing blank lines between `</table>` closing tags and the following `####` Markdown headings. Without blank lines, Markdown parsers treat the heading as part of the preceding HTML block and do not render it as a section header, breaking navigation and rendering.

## Before
```markdown
</table>
#### Next Section
```

## After
```markdown
</table>

#### Next Section
```

## How to spot
Render the documentation locally (`mkdocs serve`) and check that all section headings are visually separated. Also run an HTML/Markdown lint tool that flags headings immediately following HTML blocks.

## Test
Documentation rendering verified by inspection.
