## Commit

- Hash: d1b1d9d47d
- Tickets: KAFKA-18111
- Subject: KAFKA-18111: Add Kafka logo to project README

## Subsystem

docs / README

## Category

Logic error in condition

## Root cause

The Apache Kafka `README.md` lacked any visual project identity — it displayed plain text with no logo. While not a correctness bug, the absence of the official logo made the repository landing page look unpolished compared to other Apache projects and made it harder to confirm at a glance that the repository is the official one. New SVG logo files were added and referenced from the README.

## Before

README had no logo image; only plain-text project name.

## After

Apache Kafka SVG logo files added under `docs/`; `README.md` updated with an `<img>` reference to the logo.

## How to spot

Repository READMEs for established projects that lack a logo or visual identifier — low-priority cosmetic, but worth fixing for project professionalism.

## Test

Documentation/cosmetic change only; no test.
