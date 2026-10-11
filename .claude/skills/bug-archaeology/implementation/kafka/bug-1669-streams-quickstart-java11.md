## Commit

- Hash: 0e831d0840
- Tickets: KAFKA-8116
- Subject: KAFKA-8116: Update Streams quickstart archetype POM to Java 11 and SLF4J 2.x

## Subsystem

streams / quickstart / Maven archetype

## Category

Wrong constant / default value

## Root cause

The Kafka Streams quickstart Maven archetype `pom.xml` specified `slf4j.version` 1.7.36 (EOL), Java `source/target` 1.8 (below Kafka's minimum supported Java 11), and included the Eclipse JDT compiler plugin which is not needed for standard Maven builds. Generated projects would fail on Java 17+ due to the obsolete `source/target` format and would pull in an outdated SLF4J that conflicts with any SLF4J 2.x dependency in the parent project.

## Before

```xml
<slf4j.version>1.7.36</slf4j.version>
...
<source>1.8</source>
<target>1.8</target>
```

## After

```xml
<slf4j.version>2.0.16</slf4j.version>
...
<release>11</release>
```

JDT compiler plugin removed.

## How to spot

Archetype or template POMs using `<source>/<target>` notation with Java 8 when the surrounding project requires Java 11+, or dependency versions that diverge significantly from the rest of the project's BOM.

## Test

Quickstart archetype generate-and-build smoke test passes on Java 11+.
