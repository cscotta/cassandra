## Commit

- Hash: 403fe71c5f
- Tickets: KAFKA-18400
- Subject: KAFKA-18400: Fix incorrect date format string in consumer/share group command options

## Subsystem

tools / ConsumerGroupCommandOptions, ShareGroupCommandOptions, StreamsResetter

## Category

Wrong constant / default value

## Root cause

The date format string used for `--to-datetime` and similar options was `YYYY-MM-DDTHH:mm:SS.sss`. `YYYY` is the ISO week-year (not the calendar year) and produces wrong results for dates near year boundaries. `SS` is fractional seconds (wrong — should be seconds `ss`). Combined, the format silently produced incorrect timestamps that were accepted without error but mapped to wrong offsets.

## Before

```java
private static final String DATE_FORMAT = "YYYY-MM-DDTHH:mm:SS.sss";
```

## After

```java
private static final String DATE_FORMAT = "YYYY-MM-DDThh:mm:ss.sss";
```

(`YYYY` → calendar year `yyyy` is the intended final fix; `SS` → `ss` corrected here.)

## How to spot

Date/time format strings containing `YYYY` (ISO week-year) where calendar year `yyyy` was intended, or `SS` (fractional seconds) where seconds `ss` were intended. These compile and run silently but produce wrong values for boundary dates.

## Test

`ConsumerGroupCommandTest` and `ShareGroupCommandTest` extended with date-format parsing assertions around year boundaries.
