**Commit**: 016a4a6c4c · **Ticket**: KAFKA-19353
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
This is the initial documentation for KIP-932 preview in AK 4.1. The aim
is to get very minimal docs in before the cutoff. Longer term, more
comprehensive documentation will be provided for AK 4.2. The PR includes:
* Generation of group-level configuration documentation
* Add link to KafkaShareConsumer to API docs
* Add a summary of share group rational to design docs
* Add basic operations information for share groups to ops docs
* Add upgrade note describing arrival of KIP-932 preview in 4.1

Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>

---------

Co-authored-by: Apoorv Mittal <apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
                               'genKafkaConfigDocs', 'genTopicConfigDocs',
	Examples showing how to use the producer are given in the
	To use the producer, you can use the following maven dependency:
	Examples showing how to use the consumer are given in the
	To use the consumer, you can use the following maven dependency:
	<h3 class="anchor-heading"><a id="streamsapi" class="anchor-link"></a><a href="#streamsapi">2.3 Streams API</a></h3>
	Examples showing how to use this library are given in the
	To use Kafka Streams you can use the following maven dependency:

// After
  task genGroupConfigDocs(type: JavaExec) {
    classpath = sourceSets.main.runtimeClasspath
    mainClass = 'org.apache.kafka.coordinator.group.GroupConfig'
    if( !generatedDocsDir.exists() ) { generatedDocsDir.mkdirs() }
    standardOutput = new File(generatedDocsDir, "group_config.html").newOutputStream()
  }

                               'genKafkaConfigDocs', 'genTopicConfigDocs', 'genGroupConfigDocs',
	Examples of using the producer are shown in the
	To use the producer, add the following Maven dependency to your project:
	Examples of using the consumer are shown in the
	To use the consumer, add the following Maven dependency to your project:
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19353: upgrade note and initial docs for kip-932 (#19863).

**Test**
No test added.
