# Structure matcher

*Check whether two complex Java objects are "equal enough", and get a per-field report of every difference.*

Structure matcher compares two POJOs property by property, and returns a feedback tree. Per-property rules let you loosen the comparison where it should be loose: a value in a range, a string matching a regex, an ignored field, an order-insensitive list. The result serializes to JSON, so you can store and diff large numbers of comparisons.

It fits one job well: validating that two object streams are equivalent, at scale, with a localized report. Think API v1-vs-v2 contract checks, data-pipeline regression, or cross-system reconciliation, where you want to see which fields diverge

Use it when your objects have no meaningful `equals` (or can't have one), some fields need loose matching, and you want the difference reported field by field.

## Quick start

Compare two objects and inspect the feedback. Empty feedback means they matched.

```java
FeedbackNode feedback = ObjectMatcher.forClass(Book.class)
        .match(expected, actual);

if (feedback.isEmpty()) {
    // the two books matched
}
```

By default every property is compared for equality, recursing into nested structures, lists, maps, sets, arrays, and `Optional`. Register custom matchers to loosen specific fields:

```java
FeedbackNode feedback = ObjectMatcher.forClass(BookSearchResult.class)
        .with(IntegerMatchers.inRange(2, 5000),
                BookSearchResult::getMetadata, SearchMetadata::getProcessingTimeMs)
        .with(StringMatchers.regex(IPADDRESS_PATTERN),
                BookSearchResult::getMetadata, SearchMetadata::getServer, Server::ip)
        .match(expected, actual);
```

## Modules

- **core** - the library. Zero runtime dependencies. All matching logic lives here.
- **json** - serialize feedback to JSON, in two shapes: a readable rendering, and a stable versioned archive for persistence.
- **report** - aggregate many comparisons into a batch summary, and query a single result tree. - **assertj** - an AssertJ assertion: `assertThat(actual).matchesStructure(expected, spec)`.
- **junit5** - plain JUnit 5 helpers: `StructAssertions.assertMatches(expected, actual, spec)`.
- **examples** - the runnable bookstore scenario shown below.

## Example

Imagine a bookstore REST API. The search endpoint returns the books that match a query. The legacy version serves a traditional website and returns XML. The new version serves mobile devices and returns JSON. The mobile response is lighter, so it omits some properties and shortens others, like the author's first name. Here is the same result in both versions:

```xml
<BookSearchResult>
  <metadata>
    <keywords>
      <keywords>smoke</keywords>
    </keywords>
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
      <yearPublished>1999</yearPublished>
    </books>
    <books>
      <title>Summer and smoke</title>
      <authors>
        <authors>
          <firstName>Tennessee</firstName>
          <lastName>Williams</lastName>
        </authors>
      </authors>
      <yearPublished>1950</yearPublished>
    </books>
  </books>
</BookSearchResult>
```
```json
{
  "metadata" : {
    "keywords" : [ "smoke" ],
    "booksFound" : 2,
    "processingTimeMs" : 9,
    "server" : {
      "ip" : "192.168.10.45",
      "port" : 8080
    }
  },
  "books" : [ {
    "title" : "Blood and Smoke",
    "authors" : [ {
      "firstName" : "S.",
      "lastName" : "King"
    } ]
  }, {
    "title" : "Summer and smoke",
    "authors" : [ {
      "firstName" : "T.",
      "lastName" : "Williams"
    } ]
  } ]
}
```

How do we prove the two responses carry the same information?

First, we need POJOs that model the response, so we can read the XML or JSON into them (with Jackson, for instance). The simplest classes are omitted:

```java
public record Book(String title, List<Author> authors, String meta, PublishingInfo publishingInfo) {}

// A record works just as well as a classic bean. Its components are discovered as properties.
public record Server(String ip, int port) {}

public class SearchMetadata {}

public record BookSearchResult(SearchMetadata metadata, List<Book> books) {}
```

Second, we tell Structure matcher about every logical difference between the responses, then ask it to compare them.

A custom matcher is attached to a property by **path**. There are two ways to write a path:

- **Typed accessor chains** (preferred) - a sequence of method references such as `BookSearchResult::getMetadata, SearchMetadata::getServer, Server::ip`. They are checked by the compiler, completed by the IDE, and survive renames. A chain mixes bean getters and record accessors freely.
- **Dot-separated strings** - `"Metadata.Server.Ip"`. Loosely typed, but the only way to express paths that descend *into* collection elements (`"Books.Authors.FirstName"`) or use the `*` wildcard.

The two styles produce identical paths and can be mixed in one set-up.

```java
BookSearchResult desktopResponse = // read XML response
BookSearchResult mobileResponse = // read JSON response
FeedbackNode feedback = ObjectMatcher.forClass(BookSearchResult.class)
        // Typed accessor chains are refactor-safe; this one descends a bean (SearchMetadata)
        // into a record (Server) transparently.
        .with(StringMatchers.regex(IPADDRESS_PATTERN),
                BookSearchResult::getMetadata, SearchMetadata::getServer, Server::ip) // both properties are valid IP addresses
        .with(IntegerMatchers.oneOf(8080, 8081, 8090, 8091),
                BookSearchResult::getMetadata, SearchMetadata::getServer, Server::port) // the port is one of the listed values
        .with(IntegerMatchers.inRange(2, 5000),
                BookSearchResult::getMetadata, SearchMetadata::getProcessingTimeMs) // the processing time is a reasonable number
        // String path, needed here because it traverses into each element of the Books list.
        .with(Matchers.and(  // verifies that all the suppositions below are correct
                Matchers.nonNull(), // the property is present...
                StringMatchers.nonEmpty(), // ...and is a non-empty string
                Matchers.normalizingBase(name -> name.substring(0, 1) + ".", valuesEqual()) // ...and matches once the first name is shortened to an initial
              ),  "Books.Authors.FirstName") // the initials match
        .with(Matchers.constant(null), "Books.YearPublished") // the publishing year is absent in the target response
        .match(desktopResponse, mobileResponse); // match the mobile response against the desktop one
assertTrue(feedback.isEmpty()); // correct for this example
```

Only the exceptions are defined. Every other property is compared automatically, in the standard way.

## Beyond a single comparison: the batch report

`match` returns a `FeedbackNode` tree, not a boolean, so a *batch* of comparisons can be rolled up to show **which fields systematically diverge**. That is the point of validating two object streams for *equivalence at scale*: API v1-vs-v2 contract checks, data-pipeline regression, cross-system reconciliation, where you want a localized report, not pass/fail.

The `report` module aggregates many results into a `FeedbackSummary`. Replay a set of search queries against the legacy and the new API, then compare each paired response:

```java
import nl.alexeyu.structmatcher.report.FeedbackAggregator;
import nl.alexeyu.structmatcher.report.FeedbackSummary;

// `matcher` is the tolerant spec configured in the example above.
FeedbackSummary summary = FeedbackAggregator.summarize(searches.stream()
        .map(query -> matcher.match(legacyApi.search(query), mobileApi.search(query)))
        .toList());

summary.total();                      // one comparison per query
summary.mismatchRate();               // fraction whose responses diverged
summary.failureCount("Book.title");   // responses that differed on the server IP
summary.failureRate("Book.title");    // the same as a fraction of the batch
summary.topMismatchingFields(3);      // the fields diverging most often, worst first
```

A field is counted at most once per comparison, and collection indices collapse to a single field (`Books[0].Meta` and `Books[1].Meta` become `Books[].Meta`), so a rate reads as "the fraction of comparisons in which this field broke."

To inspect one comparison, `FeedbackQuery` walks the tree down to its broken leaves, each carrying its path plus the expected and actual values:

```java
import nl.alexeyu.structmatcher.report.FeedbackQuery;

var feedback = matcher.match(legacyResponse, mobileResponse);
FeedbackQuery.brokenLeaves(feedback);                           // every broken (path, expectation, value)
FeedbackQuery.mismatchesUnder(feedback, "Book.PublishingInfo"); // only the leaves under a given path
```

## Serializing and persisting feedback

Two JSON shapes for two jobs, both in the `json` module (which adds Jackson):

- **Human-readable rendering** - `Json.mapper()` serializes a `FeedbackNode` tree to nested, property-keyed JSON, for reading or diffing a single comparison.
- **Stable persistence format** - `FeedbackArchives` writes a flat, **versioned** archive (`{schemaVersion, matched, brokenLeaves:[{path, expectation, value}]}`): the format to store and reload. The reader rejects an unknown `schemaVersion` and ignores unknown fields, so additive changes stay forward-compatible.

Because the archive keeps each broken path, a persisted batch can be reloaded and aggregated **without re-running the comparisons**. Feed the stored paths back through `FeedbackAggregator.addBrokenPaths`:

```java
String stored = FeedbackArchives.toJson(matcher.match(legacyResponse, mobileResponse));
// … later, in another process, after loading many such documents …
var aggregator = new FeedbackAggregator();
aggregator.addBrokenPaths(FeedbackArchives.fromJson(stored).brokenPaths());
FeedbackSummary summary = aggregator.summary();
```

The full runnable scenario (aggregate, query, persist + reload) is `BatchReportTest` in the `examples` module.

## Using it in tests

Both bridges run the same comparison and, on a mismatch, fail with the per-field diff. Pick the one that matches your test stack:

```java
// AssertJ
StructMatcherAssertions.assertThat(actual).matchesStructure(expected, spec);

// Plain JUnit 5
StructAssertions.assertMatches(expected, actual, spec);
```

## At a glance

- **Lightweight** - `core` and `report` have no runtime dependencies; only `json` (and `examples`) pull in Jackson.
- **Flexible** - assign any matcher to any property of a composite object.
- **Refactor-safe** - attach matchers with typed accessor chains (`Server::ip`) checked by the compiler, not just dot-separated strings.
- **Extensible** - write your own matchers, or use the two dozen that ship built in.
- **Scalable** - results aggregate into a batch report and persist to a stable, versioned JSON format, so a stored batch reloads and rolls up without re-running the comparisons.
