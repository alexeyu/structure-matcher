# Structure matcher

[![build](https://github.com/alexeyu/structure-matcher/actions/workflows/build.yml/badge.svg)](https://github.com/alexeyu/structure-matcher/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.alexeyu/structure-matcher-core)](https://central.sonatype.com/artifact/io.github.alexeyu/structure-matcher-core)
[![javadoc](https://img.shields.io/badge/javadoc-2.1-blue.svg)](https://javadoc.io/doc/io.github.alexeyu/structure-matcher-core)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Java 17+](https://img.shields.io/badge/Java-17%2B-orange)

*Check whether two complex Java objects are "equal enough", and get a per-field report of every difference.*

Structure matcher compares two POJOs property by property, and returns a feedback tree. Per-property rules let you loosen the comparison where it should be loose: a value in a range, a string matching a regex, an ignored field, an order-insensitive list. A rule can also mask a field, comparing it as strictly as before but no longer printing it. The result serializes to JSON, so you can store and diff large numbers of comparisons.

Use it when your objects have no meaningful `equals` (or can't have one), some fields need loose matching, and you want the difference reported field by field. That covers API v1-vs-v2 contract checks, data-pipeline regression and cross-system reconciliation, where you compare two streams of objects and want to see which fields diverge.

For a single test assertion, reach for AssertJ's `usingRecursiveComparison()` instead. [How this compares to JaVers, AssertJ and json-unit](COMPARISON.md) says where each one wins.

## Installation

Available on Maven Central. `core` is all you need to compare objects; add the optional modules for JSON persistence, batch reporting, or a test-framework bridge.

Gradle:

```groovy
implementation 'io.github.alexeyu:structure-matcher-core:2.1'

// optional add-ons, described below
implementation     'io.github.alexeyu:structure-matcher-json:2.1'
implementation     'io.github.alexeyu:structure-matcher-report:2.1'
testImplementation 'io.github.alexeyu:structure-matcher-assertj:2.1'
testImplementation 'io.github.alexeyu:structure-matcher-junit5:2.1'
```

Maven:

```xml
<dependency>
    <groupId>io.github.alexeyu</groupId>
    <artifactId>structure-matcher-core</artifactId>
    <version>2.1</version>
</dependency>
```

The Java packages are `nl.alexeyu.structmatcher.*` regardless of the `io.github.alexeyu` coordinate namespace.

## Quick start

Compare two objects and inspect the feedback. Empty feedback means they matched.

```java
FeedbackNode feedback = ObjectMatcher.forClass(Book.class)
        .match(expected, actual);

if (feedback.isEmpty()) {
    // the two books matched
}
```

By default `ObjectMatcher` compares every property for equality, recursing into nested structures, lists, maps, sets, arrays, and `Optional`. Register custom matchers to loosen specific fields, as the bookstore example does.

## Modules

- **core** - the library. Zero runtime dependencies. All matching logic lives here.
- **json** - serialize feedback to JSON, in two shapes: a readable rendering, and a stable versioned archive for persistence.
- **report** - aggregate many comparisons into a batch summary, and query a single result tree.
- **assertj** - an AssertJ assertion: `assertThat(actual).matchesStructure(expected, spec)`.
- **junit5** - plain JUnit 5 helpers: `StructAssertions.assertMatches(expected, actual, spec)`.
- **examples** - the runnable bookstore scenario shown below.

## Example

Imagine a bookstore REST API. The search endpoint returns the books that match a query. The legacy version serves a traditional website and returns XML. The new version serves mobile devices and returns JSON. The mobile response is lighter, so it omits some properties and shortens others, like the author's first name. Here is the same result in both versions, with the second of the two books elided:

```xml
<BookSearchResult>
  <metadata>
    <booksFound>2</booksFound>
    <processingTimeMs>14</processingTimeMs>
    <server>
      <ip>192.168.10.14</ip>
      <port>8081</port>
    </server>
  </metadata>
  <books>
    <books>
      <title>Blood and Smoke</title>
      <authors>
        <authors>
          <firstName>Stephen</firstName>
          <lastName>King</lastName>
        </authors>
      </authors>
      <publishingInfo>
        <publisher>Simon &amp; Schuster Audio</publisher>
        <year>1999</year>
        <length>96</length>
      </publishingInfo>
    </books>
  </books>
</BookSearchResult>
```
```json
{
  "metadata" : {
    "booksFound" : 2,
    "processingTimeMs" : 9,
    "server" : { "ip" : "192.168.10.45", "port" : 8080 }
  },
  "books" : [ {
    "title" : "Blood and Smoke",
    "authors" : [ { "firstName" : "S.", "lastName" : "King" } ]
  } ]
}
```

To prove the two responses carry the same information, start with POJOs that model the response, so you can read the XML or JSON into them (with Jackson, for instance). The simplest classes are omitted:

```java
public record Book(String title, List<Author> authors, PublishingInfo publishingInfo) {}

public record PublishingInfo(String publisher, int year, int length) {}

// A record works as well as a classic bean. Its components become properties.
public record Server(String ip, int port) {}

// A classic bean, to show the two styles mixing in one path.
public class SearchMetadata {}

public record BookSearchResult(SearchMetadata metadata, List<Book> books) {}
```

Then declare every logical difference between the responses and run the comparison.

You attach a custom matcher to a property by **path**, written either way:

- **Typed accessor chains** (preferred) - a sequence of method references such as `BookSearchResult::metadata, SearchMetadata::getServer, Server::ip`. The compiler checks them, the IDE completes them, and they survive a rename. One chain can mix bean getters and record accessors.
- **Dot-separated strings** - `"Metadata.Server.Ip"`. Loosely typed, but the only way to express paths that descend *into* collection elements (`"Books.Authors.FirstName"`) or use the `*` wildcard.

Both styles produce identical paths, and one set-up can mix them.

Register `"*.Url"` to relax every URL in the model, then `"Metadata.Server.Url"` to take that one back. When two paths match the same property, the more specific one wins: fewest wildcards, then most named segments, then the longest run of names before the first wildcard.

```java
BookSearchResult desktopResponse = // read XML response
BookSearchResult mobileResponse = // read JSON response
FeedbackNode feedback = ObjectMatcher.forClass(BookSearchResult.class)
        // Typed accessor chains are refactor-safe; this one descends a bean (SearchMetadata)
        // into a record (Server) transparently.
        .with(StringMatchers.regex(IPADDRESS_PATTERN),
                BookSearchResult::metadata, SearchMetadata::getServer, Server::ip) // both properties are valid IP addresses
        .with(IntegerMatchers.oneOf(8080, 8081, 8090, 8091),
                BookSearchResult::metadata, SearchMetadata::getServer, Server::port) // the port is one of the listed values
        .with(IntegerMatchers.inRange(2, 5000),
                BookSearchResult::metadata, SearchMetadata::getProcessingTimeMs) // the processing time is a reasonable number
        // Matchers compose fluently, left to right.
        .with(Matchers.<String>nonNull()
                .and(StringMatchers.nonEmpty()) // present and a non-empty string...
                .and(Matchers.<String>valuesEqual()
                        .normalizingBoth(name -> name.charAt(0) + ".")), // ...and the same once reduced to an initial
                "Books.Authors.FirstName") // "Stephen" and "S." are the same author
        .with(absentOrEqual(), "Books.PublishingInfo") // absent is fine, present must be right
        .match(desktopResponse, mobileResponse); // match the mobile response against the desktop one
assertTrue(feedback.isEmpty()); // the two responses carry the same answer
```

Two details:

- `normalizingBoth`, not `normalizingBase`. Reducing only the baseline would make `Stephen` match `S.` but stop `Stephen` matching `Stephen`, so the spec would fail on any desktop-to-desktop comparison. Reducing both sides makes one spec serve every pairing, and it stays strict: `Stephen` still does not match `T.`.
- `absentOrEqual` tolerates *absence*, not *anything*. A response that omits `publishingInfo` passes; one that returns the wrong year does not. A matcher is a lambda, so a rule the built-ins don't cover takes a few lines:

  ```java
  static Matcher<PublishingInfo> absentOrEqual() {
      return (property, expected, actual) -> actual == null
              ? Feedback.empty(property)
              : Matchers.<PublishingInfo>structuresEqual().match(property, expected, actual);
  }
  ```

You define only the deviations. Structure matcher compares every other property in the standard way.

## Beyond a single comparison: the batch report

`match` returns a `FeedbackNode` tree rather than a boolean, so you can roll a *batch* of comparisons up into a view of **which fields systematically diverge**, which is what an API v1-vs-v2 contract check or a data-pipeline regression run needs. Replay a set of queries against both APIs and aggregate the results:

```java
// `matcher` is the tolerant spec from the example above.
FeedbackSummary summary = FeedbackAggregator.summarize(searches.stream()
        .map(query -> matcher.match(legacyApi.search(query), mobileApi.search(query)))
        .toList());
```

`summary.toString()` is the digest, worst field first:

```
200 comparisons: 170 matched, 30 mismatched (15.0%)
  Books[].Title: 19 (9.5%)
  Metadata.BooksFound: 10 (5.0%)
  Books[].Authors[].LastName: 3 (1.5%)
```

The digest names the three fields behind those 30 mismatches and how often each one breaks. Every response in this batch came from a different host and port, took a different time, abbreviated the author first names and omitted the publishing details; the spec tolerates all of that, so none of it shows up here.

[Batch reporting and persistence](docs/reporting.md) covers the rest: per-field counts and rates, `FeedbackQuery` for walking one tree down to its broken leaves, the versioned JSON archive, and reloading a stored batch without re-running the comparisons.

## Masking sensitive values

`anyValue()` is the blunt tool for a field you would rather not print: it stops checking it. Masking keeps the check and drops the printing - **a masked field is compared as strictly as any other**:

```java
FeedbackNode feedback = ObjectMatcher.forClass(Order.class)
        .with(Matchers.valuesEqual().masking(Maskers.hash()), "*.Email")
        .with(Matchers.structuresEqual().masking(Maskers.redacted()), "Customer.Address")
        .match(expectedOrder, actualOrder);
// Customer: [Email: sha256:5ff860bf1190596c !~ sha256:ff8d9819fc0e12bf]
```

`masking` is a default method on `Matcher`, alongside `normalizing*`, so it composes with any rule and registers by path like any other. `Maskers` offers `redacted()`, `fixed(text)`, `hash()` and `keepingFirst(n)` / `keepingLast(n)`. Prefer `hash()` for a batch: equal values stay equal, so the report still counts how often a field diverges, and different values stay different, so you can see that they did.

Masking is redaction, opt-in per path: it covers the paths you name and nothing else. [The masking rules](docs/masking.md) spell out what stays readable, how far a mask spreads through the tree, and how composing in the wrong order drops it.

## Using it in tests

Both bridges run the same comparison and, on a mismatch, fail with the per-field diff. Pick the one matching your test stack:

```java
// AssertJ
StructMatcherAssertions.assertThat(actual).matchesStructure(expected, spec);

// Plain JUnit 5
StructAssertions.assertMatches(expected, actual, spec);
```

The failure message lists each diverging field instead of dumping two objects for you to compare:

```
2 field(s) did not match:
  [Metadata.BooksFound] expected: <2> but was: <3>
  [Books[0].Title] expected: <Blood and Smoke> but was: <Blood & Smoke>
```

The AssertJ bridge prints the same per-field list, preceded by both objects in AssertJ's usual style. The JUnit 5 helper attaches them to the `AssertionFailedError`, so the IDE still offers its comparison view. A spec that [masks values](#masking-sensitive-values) gets the list alone: rendering the objects would print what the masked leaves withhold.

## Reading your model

The library reads properties by reflection, through accessors: a no-arg `getX()` / `isX()` on a bean, the components of a `record`. It reads methods only and does not widen access, so the accessor has to be reachable from outside your package: a public declaring class, or a public supertype declaring the same accessor. AutoValue and Immutables pass on the second rule.

[Model requirements](docs/model-requirements.md) lists the property shapes the library reads, the `InaccessibleAccessorException` you get when no accessor is reachable, and the way around it.

## One comparison, one thread

`match()` runs on one thread from start to finish, holding the current property path in a thread-local. That path is how the library finds a rule you registered deep in the model. Capture a `Matcher` and invoke it on another thread and it sees an empty path: the comparison still runs, your custom matchers no longer apply, and nothing warns you. Keep one comparison on one thread.

Many `match()` calls in parallel are fine, each with its own state on its own thread, which is the parallelism you want for a batch of thousands of comparisons.
