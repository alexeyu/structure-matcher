# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A lightweight Java library for comparing two complex POJOs that lack (or cannot have) a meaningful `equals`. Comparison runs property-by-property via reflection and returns a `FeedbackNode` tree describing every mismatch, rather than a single boolean. Per-property rules ("matchers") let you loosen the comparison (range checks, regex, normalization, ignored fields, order-insensitive lists). The result tree can be serialized to JSON for storing/diffing large numbers of comparisons.

## Build & test

Use the committed **Gradle wrapper** (`./gradlew`, Gradle 8.10.2). Java toolchain is **17**. The build uses `java-library` + `maven-publish` with `api`/`implementation`/`testImplementation` configurations.

```bash
./gradlew build              # compile + test all modules (also runs spotlessCheck)
./gradlew :core:test         # test a single module (core | json | report | examples)
./gradlew test --tests nl.alexeyu.structmatcher.matcher.ObjectMatcherTest          # single test class
./gradlew test --tests nl.alexeyu.structmatcher.matcher.ObjectMatcherTest.someMethod  # single test method
./gradlew spotlessApply      # auto-fix formatting; spotlessCheck verifies (and runs in CI via build)
```

**Formatting:** light-touch Spotless (configured in the root `build.gradle`) — it orders imports (static, then `java`/third-party/`nl` groups), removes unused imports, trims trailing whitespace, and enforces a final newline. It does **not** reformat indentation or wrapping, so the existing 4-space style is preserved. (The source is currently wrapped at ~100 columns, but that is not enforced.) Run `./gradlew spotlessApply` before committing; `spotlessCheck` runs as part of `build` (so CI enforces it).

Tests run on the **JUnit 5 Platform**, but the test sources are still JUnit 4 (`org.junit.Test`, one `@RunWith(Theories.class)`, Mockito's `MockitoJUnitRunner`) executed via the **JUnit Vintage engine**. Per-test migration to Jupiter is deferred (see ROADMAP Phase 0/1). Mockito is used in one test; `json-path` and (in `examples`) Jackson XML/JSON load fixtures.

## Module layout

- **core** — the library. Zero runtime dependencies. All matching logic lives here.
- **json** — depends on `core` + Jackson. `Json.mapper()` returns a configured `ObjectMapper` that serializes a `FeedbackNode` tree to JSON (custom serializer for composite nodes; a mixin hides internal fields).
- **report** — depends on `core` (zero extra runtime deps). Consumes the `FeedbackNode` tree to produce corpus-level insight: flatten broken paths and aggregate N comparisons into a `FeedbackSummary`. Kept separate from core (like `json`) so the matching core stays a pure "produce feedback" library and the analysis surface can grow.
- **examples** — runnable end-to-end usage (the bookstore XML-vs-JSON scenario from the README). Model POJOs and integration tests live together under `examples/.../bookstore`.

## Architecture

### Entry point and the four-step flow
`ObjectMatcher.forClass(T.class)` → register custom matchers with `.with(matcher, "Dot.Separated.Path")` or `.withMatcher(matcher, "Path", "Parts")` → `.match(expected, actual)` → inspect the returned `FeedbackNode` (`feedback.isEmpty()` means they matched). See `matcher/ObjectMatcher.java`.

### Properties come from getters (or record components), by reflection
For a classic bean, a "property" is a no-arg `getX()`/`isX()` method (`getClass` excluded), named by stripping the prefix. For a **`record`**, properties are its components (via `getRecordComponents()`), named by the component accessor. Both are **capitalized** so paths are identical regardless of model style: `getServer().getIp()` and `server().ip()` both give path `Server.Ip`. See `property/ClassProperty.java` (the `recordComponent` flag distinguishes the two so a component literally named `getX` isn't mis-stripped). Supported shapes: **simple** values (primitives, `Number`, `Boolean`, `Character`, `String`, enums), **`List`s**, **`Map`s**, **`Set`s**, **arrays** (of objects or primitives), **`Optional`** (treated as a nullable value), and **structures** (everything else, recursed into).

### Default matcher selection
For each property, `Matchers.forProperty` picks a default: `valuesEqual()` for simple, `listsEqual()` for lists, `mapsEqual()` for maps, `setsEqual()` for sets, `arraysEqual()` for arrays, `optional()` for `Optional`, `structuresEqual()` for structures (which recurses). Most matchers are wrapped `nullAware` (both-null matches; one-null is a mismatch). The collection matchers (`ListMatcher`/`MapMatcher`/`SetMatcher`/`ArrayMatcher`) are *not* null-aware (the collection must be non-null) and report sub-nodes under `property[index]` / `property[key]` / `property[element]`. `MapMatcher` and `SetMatcher` additionally report missing/extra keys/elements. `ArrayMatcher` adapts the array to a `List` via reflection (`java.lang.reflect.Array`, so primitive arrays auto-box) and delegates to `ListMatcher`. `OptionalMatcher` unwraps both `Optional`s to value-or-`null` (empty ≈ `null`) and matches the contents with the default logic. **`SetMatcher` compares elements by their own `equals`/`hashCode` (membership), not field by field** — so value types (records, strings, enums) are the natural set elements; `IgnoreOrderListMatcher` remains the comparator-based, field-aware option for order-insensitive *lists*.

A known limitation across all collection matchers: a collection whose *elements/values are themselves collections* (list-of-list, map-of-array, …) isn't deeply matched — `Matchers.forObject` routes non-simple elements to `structuresEqual`, not back to the collection matchers (see ROADMAP).

### The matching stack and custom-matcher resolution (the key mechanism)
Matching is driven by a `ThreadLocal` `MatchingStack` (`MatchingStackHolder`), set up in `ObjectMatcher.match` and cleared in a `finally`. As the matcher tree descends into nested properties, `ContextAwareMatcher` pushes the current property name onto a `PropertyPath`. At each node, `DefaultMatchingStack.push` asks `WildcardMatcherResolver` whether a registered custom matcher's `PropertyPathPattern` matches the current path; if so it overrides the default, otherwise the default supplier is used. The stack also holds the top-level base/actual structures so indirect matchers can reach across the tree. **Implication:** matching is not thread-safe per `ObjectMatcher` call in the sense that it relies on thread-local state for the duration of one `match()` — keep matching synchronous within a thread.

### Path patterns support wildcards
A registered path may contain `*`, matching any run of intermediate properties — e.g. `*,Url` applies a URL matcher to every `Url` property anywhere, `A,*,C` matches `A,B,C`. Recursion logic and the full match/no-match table are in `matcher/WildcardPathChecker.java`.

### Indirect matchers are special-cased
A normal matcher receives `(propertyName, expectedValue, actualValue)` — values already extracted for that one property. An `IndirectMatcher` instead receives the **whole base and actual structures** and uses two fetcher functions to derive the values to compare. `ContextAwareMatcher` detects `instanceof IndirectMatcher` and feeds it the top-level structures from the stack rather than the local property values. Use this when the expected value for one field depends on a different part of the object. Created via `Matchers.indirectMatcher(...)`.

### Matcher composition
Matchers are a `@FunctionalInterface` (`FeedbackNode match(String, V, V)`). The `Matchers`, `StringMatchers`, and `IntegerMatchers` factories produce ~two dozen ready matchers. Compose them with `and(...)` (first non-empty feedback wins), the `normalizing` / `normalizingBase` / `normalizingBoth` wrappers (apply a `UnaryOperator` before delegating), `constant(v)`, `anyValue()`, `nonNull()`, `mustConform(predicate, spec)`, the list matchers `listsEqual()` / `listsHaveEqualElements(comparator)`, the map matcher `mapsEqual()`, the set matcher `setsEqual()`, the array matcher `arraysEqual()`, and the optional matcher `optional()`. To add a new built-in matcher, implement `Matcher<V>` (or return a lambda from a factory) and expose it through the relevant factory class.

### Feedback tree
Matchers return `FeedbackNode`s built by the `feedback/Feedback` factory: `ExpectationMet` (empty/leaf-OK), `ExpectationBroken` (leaf mismatch with expected/actual/spec), and `CompositeFeedbackNode` (a structure's children — empty only if all children empty). A `BrokenSpecificationException` is thrown when a *base* value violates a strict matcher's precondition (e.g. base is null under `nonNull()`), signaling a bad spec rather than a data mismatch.

## Reporting / aggregation (the `report` module)
The `report` module (`nl.alexeyu.structmatcher.report`, depends on `core` only) turns feedback into corpus-level insight (Phase 3). `FeedbackPaths.brokenPaths(node)` flattens a tree into registration-style paths (drops the root class name, joins structure children with `.`, keeps collection brackets without repeating the parent name: `Sub.Bool`, `Strings[0]`, `Books[0].Authors[0].FirstName`); `FeedbackPaths.toFieldPath` collapses `[index]`/`[key]`/`[element]` to `[]`. `FeedbackAggregator` (`add`/`addAll`/`summary`, or one-shot static `summarize`) accumulates many comparisons into a `FeedbackSummary`: total / matched / mismatched, `mismatchRate`, per-field failure counts and rates (a field is counted at most once per comparison, ordered most-failing first), and `topMismatchingFields`. Zero extra runtime deps; not thread-safe (aggregate from one thread). Its end-to-end test uses a small local model (`SampleStructure`) because core's test fixtures aren't visible across the module boundary.
