# Masking sensitive values

`Matcher.masking(Masker)` runs the comparison untouched and withholds the values from the feedback
it produced. Your rule still applies in full, so **a masked field stays as strict as any other**,
where `anyValue()` would stop checking it.

```java
FeedbackNode feedback = ObjectMatcher.forClass(Order.class)
        .with(Matchers.valuesEqual().masking(Maskers.hash()), "*.Email")
        .with(Matchers.structuresEqual().masking(Maskers.redacted()), "Customer.Address")
        .match(expectedOrder, actualOrder);
// Customer: [Email: sha256:5ff860bf1190596c !~ sha256:ff8d9819fc0e12bf]
```

`masking` is a default method on `Matcher`, alongside `normalizing*`, so it composes with any rule
and registers by path like any other - a wildcard reaches every `Email` in the model. `Maskers`
offers `redacted()`, `fixed(text)`, `hash()` and `keepingFirst(n)` / `keepingLast(n)`. Prefer
`hash()` for a batch: equal values stay equal, so the report still counts how often a field
diverges, and different values stay different, so you can see that they did.

## What it covers

- **Values, never the words a matcher wrote.** An expectation like `Non-null` or `Size 3` is a
  condition rather than data, and stays readable; masking both sides would leave `*** !~ ***`.
- **The whole subtree.** Masking a structure redacts every field under it, and map keys and set
  elements go with it, since an entry can be named after an identifier. A list index survives,
  since it locates the mismatch and carries nothing.
- **Ancestors that report a value whole.** If `Customer` is null on one side, the mismatch is
  reported at `Customer`, the rule on `Customer.Email` never runs, and the leaf would print the
  entire record. Where anything below a node is masked, a value that node reports whole - a
  structure or collection it never descended into, a missing map entry, a dropped list element -
  is withheld as well. Its simple fields are untouched, each having a rule of its own.
- **The assertion bridges.** A spec that masks makes `assertj` and `junit5` drop the two whole
  objects they otherwise render, which would print what the leaves withhold.

`.masking(...)` works on an `IndirectMatcher` too: it goes to the value matcher inside, so the
cross-field rule still receives the whole objects and the derived values are still withheld.

## Where it stops

Masking is **redaction, opt-in per path**: it covers the paths you name and nothing else, so mask
`Email`, miss `AlternateEmail`, and nothing warns you. And mask last, since
`masking(m).and(other)` hands back a plain matcher whose second half is unmasked, which the bridges
stop recognizing. One value escapes by design: a `BrokenSpecificationException`, thrown when the
*base* value breaks a strict matcher, still carries that value, since it reports a broken spec
rather than a mismatch.

## Masking a whole tree

`Masking.mask(feedback, masker)` redacts an entire tree without going through a matcher rule, for
a renderer or a store you feed one to directly.

For a batch that may not carry values at all, `FeedbackArchives.archiveWithoutValues(feedback)`
persists the broken paths and drops every value, including the ones a path embeds: a map or a set
names its feedback after the entry, so `Contacts[alice@example.com]` archives as `Contacts[***]`,
while a list index stays.
