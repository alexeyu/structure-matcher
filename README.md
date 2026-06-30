# Structure matcher

### This is a simple Java utility to compare complex Plain Old Java Objects.

Sometimes it is necessary to detect whether two POJOs are equal "enough".

They don't have the equals method though, and it is not possible (or difficult) to introduce it.
Moreover, sometimes the objects cannot be completely identical. For instance, they represent a complex payload of an HTTP response,
and this response contains a sequence number or processing time. So, for some parts we'd need loose matching: make sure the value
of a field is not blank or is in a certain integer range.

Finally, it might be not enough to know whether two objects are different. It would be better to see what is the difference, property by property (and properties can be POJOs themselves).

This library allows to compare two complex object, using default or custom rules, and provides feedback as another object,
which can be analyzed or easily rendered as JSON.

### Example

Imagine we have a book store REST API. The search endpoint returns a list of books which conform the user's query. 
The legacy version is written for a traditional website and returns XML. The new version, written for mobile devices, returns JSON.
Moreover, the response for mobile devices should be lighter since network throughput can be low.
So, it was decided to omit certain properties from the "mobile" response. Moreover, some other property values, like the author's
first name are shortened. There are examples of the same response for both versions:

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
How to prove the responses contain the same information?

First, supposedly we have POJOs which model a response (the most straightforward classes are ommitted).
So, it is easy to read XML/Json into it, using for instance Jackson library.
```java
public class Book {
    private String title;
    private List<Author> authors;
    private int yearPublished;
    // Constructors and getters
}

// A record works just as well as a classic bean — its components are discovered as properties.
public record Server(String ip, int port) {}

public class SearchMetadata {
    private List<String> keywords;
    private int booksFound;
    private int processingTimeMs;
    private Server server;
    // Constructors and getters
}

public class BookSearchResult {
    
    private SearchMetadata metadata;    
    private List<Book> books;
    // Constructors and getters    
}

```
Second, we can make the Structmatcher library aware of all the logical differences between the responses and ask to compare them.

A custom matcher is attached to a property by **path**. There are two ways to write a path:

* **Typed accessor chains** (preferred) — a sequence of method references such as
  `BookSearchResult::getMetadata, SearchMetadata::getServer, Server::ip`. They are checked by the
  compiler, completed by the IDE, and survive renames; a chain mixes bean getters and record
  accessors freely.
* **Dot-separated strings** — `"Metadata.Server.Ip"`. Loosely typed, but the only way to express
  paths that descend *into* collection elements (`"Books.Authors.FirstName"`) or that use the `*`
  wildcard. The two styles produce identical paths and can be mixed in one set-up.

```java
BookSearchResult desktopResponse = // read XML response
BookSearchResult mobileResponse = // read JSON response
FeedbackNode feedback = ObjectMatcher.forClass(BookSearchResult.class)
        // Typed accessor chains — refactor-safe; the chain descends a bean (SearchMetadata)
        // into a record (Server) transparently.
        .with(StringMatchers.regex(IPADDRESS_PATTERN),
                BookSearchResult::getMetadata, SearchMetadata::getServer, Server::ip) // both properties are valid IP addresses
        .with(IntegerMatchers.oneOf(8080, 8081, 8090, 8091),
                BookSearchResult::getMetadata, SearchMetadata::getServer, Server::port) // the port is one of the listed values
        .with(IntegerMatchers.inRange(2, 5000),
                BookSearchResult::getMetadata, SearchMetadata::getProcessingTimeMs) // the processing time is a reasonable number
        // String path — needed here because it traverses *into* each element of the Books list.
        .with(Matchers.and(  // verifies that all the suppositions below are correct
        	    Matchers.nonNull(), // verifies that the property is present...
                StringMatchers.nonEmpty(), // ... and is a non-empty string
                Matchers.normalizingBase(name -> name.substring(0, 1) + ".", valuesEqual()) // ...and, provided that the first name is converted to an initial
              ),  "Books.Authors.FirstName") // the initials match
        .with(Matchers.constant(null), "Books.YearPublished") // verifies that the publishing year for a target response is empty
        .match(desktopResponse, mobileResponse); // matches the mobile response against the desktop one.
assertTrue(feedback.isEmpty()); // Correct for the given example
```
Please note that only exceptions are defined. All the other properties are compared automatically, in a standard way. 

### Beyond a single comparison: the batch report

`match` returns a `FeedbackNode` tree, not a boolean — so a *batch* of comparisons can be rolled up
to show **which fields systematically diverge**. That is the point when you validate two object
streams for *equivalence at scale*: API v1-vs-v2 contract checks, data-pipeline regression,
cross-system reconciliation — where you want a localized report, not a pass/fail.

The `report` module (depends only on `core`, no extra runtime deps) aggregates many results into a
`FeedbackSummary`:

```java
import nl.alexeyu.structmatcher.report.FeedbackAggregator;
import nl.alexeyu.structmatcher.report.FeedbackSummary;

var matcher = ObjectMatcher.forClass(BookSearchResult.class);
FeedbackSummary summary = FeedbackAggregator.summarize(List.of(
        matcher.match(baseline, mobileResponse),
        matcher.match(baseline, productionResponse),
        matcher.match(baseline, baseline)));

summary.total();                              // 3
summary.matched();                            // 1
summary.mismatchRate();                       // 0.666…
summary.failureCount("Metadata.Server.Ip");   // 2  (broke in 2 of the 3 comparisons)
summary.failureRate("Metadata.Server.Ip");    // 0.666…
summary.topMismatchingFields(2);              // [Metadata.ProcessingTimeMs, Metadata.Server.Ip]
```

A field is counted at most once per comparison, and collection indices collapse to a single field
(`Books[0].Meta` and `Books[1].Meta` → `Books[].Meta`), so a rate reads as "the fraction of
comparisons in which this field broke."

To inspect one comparison, `FeedbackQuery` walks the tree down to its broken leaves — each carrying
its path plus the expected and actual values:

```java
import nl.alexeyu.structmatcher.report.FeedbackQuery;

var feedback = matcher.match(baseline, mobileResponse);
FeedbackQuery.brokenLeaves(feedback);                       // every broken (path, expectation, value)
FeedbackQuery.mismatchesUnder(feedback, "Metadata.Server"); // only the leaves under a given path
```

### Serializing and persisting feedback

Two JSON shapes for two jobs, both in the `json` module (which adds Jackson):

* **Human-readable rendering** — `Json.mapper()` serializes a `FeedbackNode` tree to nested,
  property-keyed JSON, for reading or diffing a single comparison.
* **Stable persistence format** — `FeedbackArchives` writes a flat, **versioned** archive
  (`{schemaVersion, matched, brokenLeaves:[{path, expectation, value}]}`): the format to store and
  reload. The reader rejects an unknown `schemaVersion` and ignores unknown fields (so additive
  changes stay forward-compatible).

Because the archive keeps each broken path, a persisted batch can be reloaded and aggregated
**without re-running the comparisons** — feed the stored paths back through
`FeedbackAggregator.addBrokenPaths`:

```java
String stored = FeedbackArchives.toJson(matcher.match(baseline, mobileResponse));
// … later, in another process, after loading many such documents …
var aggregator = new FeedbackAggregator();
aggregator.addBrokenPaths(FeedbackArchives.fromJson(stored).brokenPaths());
FeedbackSummary summary = aggregator.summary();
```

The full runnable scenario (aggregate, query, persist + reload) is `BatchReportTest` in the
`examples` module.

### Pros and cons

The library is: 
* lightweight - the `core` and `report` modules have no runtime dependencies at all; only `json` (and `examples`) depend on Jackson.
* flexible - you can assign any matcher to any property of a composite object;
* refactor-safe - matchers can be attached with typed accessor chains (`Server::ip`) checked by the compiler, not just dot-separated strings;
* extensible - you can write your matchers, however two dozen of them are already available,
covering many use cases.
* scalable - results aggregate into a batch report (`report` module) and persist to a stable, versioned JSON format (`json` module), so a stored batch reloads and rolls up without re-running the comparisons.

Supported property shapes: primitive and other simple values, nested structures, Java `record`s, `List`, `Set`, `Map`, arrays (object and primitive), and `Optional`.

Known limitation: a collection whose *elements/values are themselves collections* (a list of lists, a map of arrays, ...) is not matched deeply.
