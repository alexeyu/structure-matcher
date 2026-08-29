# Model requirements

The library reads properties by reflection, through accessors: a no-arg `getX()` / `isX()` on a
bean, the components of a `record`. It capitalizes the names either way, so
`getServer().getIp()` and `server().ip()` both give the path `Server.Ip`, and one path can descend
through both styles.

Supported shapes: simple values (primitives, `Number`, `Boolean`, `Character`, `String`, enums),
`List`s, `Map`s, `Set`s, arrays, `Optional` (treated as a nullable value), and structures, which it
recurses into.

## Accessors have to be reachable

The library reads methods only and does not widen access, so **the accessor has to be reachable
from outside your package**: a public declaring class, or a public supertype declaring the same
accessor. AutoValue and Immutables pass on the second rule, since their package-private subclass
inherits a public declaration and the call lands on the override.

With no public declaration anywhere, an internal DTO or a package-private test fixture fails on the
first read with `InaccessibleAccessorException`, naming the accessor and the class. It reports a
broken model, as `BrokenSpecificationException` reports a broken spec. Make the type or a supertype
public, or register a matcher on an *enclosing* path: that matcher takes the whole structure, so the
recursion stops before anything reads the property.

## A structure needs comparable properties

A type with no discoverable properties never passes by default, since empty feedback would mean
"match". The comparison falls back to the type's own `equals`, and throws
`NoComparablePropertiesException` when the type has none. Register a custom matcher on that path to
compare it your own way.
