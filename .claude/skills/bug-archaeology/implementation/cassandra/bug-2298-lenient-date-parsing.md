# Bug 2298: Date Validation Accepts Semantically Invalid Dates (Lenient Parsing)

**Commit**: 6399c94e48 · **Ticket**: n/a

## Detection Rule

When parsing dates from user input for storage or comparison, use a strict parser that rejects out-of-range field values (e.g., month 13, day 32) rather than a lenient parser that silently rolls them over to the next period.

## Code Smell

`DateUtils.parseDate(source, patterns)` (Apache Commons Lang) uses lenient parsing by default, silently accepting values like `2012-02-30` and rolling them to `2012-03-01`; `parseDateStrictly` rejects such values with a `ParseException`.

## Tags

- scope: universal
- category: wrong-filter-result
- code-pattern: lenient-date-parsing
- severity: silent-wrong-result
- subsystem: cql-query

## Root Cause

`DateType.fromString()` used `DateUtils.parseDate()` from Apache Commons Lang 2.4, which applies `Calendar.LENIENT` mode. Invalid date components (e.g., month 13, day 32) were silently normalized rather than rejected, allowing garbage dates to be stored. The fix upgrades to Commons Lang 2.6 and switches to `DateUtils.parseDateStrictly()`, which rejects any date with out-of-range fields.

## Example

```java
// BEFORE — lenient: "2012-02-30" accepted and rolled over to "2012-03-01"
millis = DateUtils.parseDate(source, iso8601Patterns).getTime();
```

```java
// AFTER — strict: "2012-02-30" throws ParseException
millis = DateUtils.parseDateStrictly(source, iso8601Patterns).getTime();
```
