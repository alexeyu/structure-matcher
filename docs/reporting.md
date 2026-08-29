# Batch reporting and persistence

`match` returns a `FeedbackNode` tree rather than a boolean, so you can roll a *batch* of
comparisons up into a view of which fields systematically diverge, and query, store or reload a
single result. Two optional modules do that work: `report` (aggregation and query, depends on
`core` only) and `json` (serialization).

## Aggregating a batch

`FeedbackAggregator` accumulates many results into a `FeedbackSummary`. Replay a set of search
queries against the legacy and the new API, then compare each paired response:

```java
import nl.alexeyu.structmatcher.report.FeedbackAggregator;
import nl.alexeyu.structmatcher.report.FeedbackSummary;

// `matcher` is the tolerant spec configured in the README example.
FeedbackSummary summary = FeedbackAggregator.summarize(searches.stream()
        .map(query -> matcher.match(legacyApi.search(query), mobileApi.search(query)))
        .toList());

summary.total();                          // one comparison per query
summary.mismatchRate();                   // fraction whose responses diverged
summary.failureCount("Books[].Title");    // responses differing on a book title
summary.failureRate("Books[].Title");     // the same as a fraction of the batch
summary.topMismatchingFields(3);          // the fields diverging most often, worst first
```

`summary.toString()` is the digest, worst field first:

```
200 comparisons: 170 matched, 30 mismatched (15.0%)
  Books[].Title: 19 (9.5%)
  Metadata.BooksFound: 10 (5.0%)
  Books[].Authors[].LastName: 3 (1.5%)
```

A field is counted at most once per comparison, and collection indices collapse to a single field
(`Books[0].Title` and `Books[1].Title` become `Books[].Title`), so a rate reads as "the fraction of
comparisons in which this field broke."

## Querying one comparison

`FeedbackQuery` walks a tree down to its broken leaves, each carrying its path plus the expected and
actual values:

```java
import nl.alexeyu.structmatcher.report.FeedbackQuery;

var feedback = matcher.match(desktopResponse, mobileResponse);
FeedbackQuery.brokenLeaves(feedback);                  // every broken (path, expectation, value)
FeedbackQuery.mismatchesUnder(feedback, "Books[0]");   // only the leaves under a given path
FeedbackQuery.find(feedback, leaf -> leaf.path().endsWith("Title")); // by predicate
```

Run the spec against a response that did regress - it claims three hits while returning two, and
renders the first title differently - and `brokenLeaves` returns those two divergences:

```
Metadata.BooksFound | 2               | 3
Books[0].Title      | Blood and Smoke | Blood & Smoke
```

That response abbreviates the author first names and drops the publishing details too, and neither
one reaches the list. Your rules suppress the differences you marked as irrelevant, and nothing
more.

## Serializing feedback

Two JSON shapes for two jobs, both in the `json` module:

- **Human-readable rendering** - `Json.mapper()` serializes a `FeedbackNode` tree to nested,
  property-keyed JSON, for reading or diffing a single comparison.
- **Stable persistence format** - `FeedbackArchives` writes a flat, **versioned** archive: the
  format to store and reload. The reader rejects an unknown `schemaVersion` and ignores unknown
  fields, so additive changes stay forward-compatible.

The archive of the comparison above:

```json
{
  "schemaVersion" : 1,
  "matched" : false,
  "brokenLeaves" : [ {
    "path" : "Metadata.BooksFound",
    "expectation" : 2,
    "value" : 3
  }, {
    "path" : "Books[0].Title",
    "expectation" : "Blood and Smoke",
    "value" : "Blood & Smoke"
  } ]
}
```

A whole batch goes to one document as JSON Lines (`toJsonLines` / `fromJsonLines`), one compact
archive per line. To store the paths without the data, use `archiveWithoutValues`, described in
[masking](masking.md).

## Reloading a batch without re-comparing

Because the archive keeps each broken path, a persisted batch can be aggregated **without
re-running the comparisons**. Feed the stored paths back through
`FeedbackAggregator.addBrokenPaths`:

```java
String stored = FeedbackArchives.toJson(matcher.match(legacyResponse, mobileResponse));
// … later, in another process, after loading many such documents …
var aggregator = new FeedbackAggregator();
aggregator.addBrokenPaths(FeedbackArchives.fromJson(stored).brokenPaths());
FeedbackSummary summary = aggregator.summary();
```

The full runnable scenario (aggregate, query, persist + reload) is `BatchReportTest` in the
`examples` module.

Aggregation is not thread-safe, so aggregate from one thread; `FeedbackQuery` is a set of pure
functions.
