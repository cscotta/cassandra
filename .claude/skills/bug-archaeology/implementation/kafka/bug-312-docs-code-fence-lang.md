## Commit
94b0f3e45f

## Ticket
KAFKA-20050

## Subsystem
Documentation

## Category
Documentation / formatting

## Root cause
Code blocks in `docs/getting-started/docker.md`, `quickstart.md`, and `zk2kraft.md` lacked language identifiers (` ```bash `, ` ```java `, etc.). Without language fences, static site generators and GitHub do not apply syntax highlighting, making the code harder to read.

## Before
````
```
./bin/kafka-topics.sh --create ...
```
````

## After
````
```bash
./bin/kafka-topics.sh --create ...
```
````

## How to spot
Grep for triple-backtick code blocks without a language tag: `grep -n '^\`\`\`$' docs/**/*.md`. Any blank-language fence should be replaced with the appropriate identifier.

## Test
Documentation rendering verified by inspection.
