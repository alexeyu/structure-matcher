# Structure matcher

### This is a simple Java utility to compare Plain Old Java Objects.

Sometimes it is necessary to detect whether two POJOs are equal "enough".

They don't have the equals method though, and it is not possible (or difficult) to introduce it.
Moreover, sometimes the objects cannot be completely identical. For instance, they represent a complex payload of an HTTP response,
and this response contains a sequence number or processing time. So, for some parts we'd need loose matching: make sure the value
of a field is not blank or is in certain range.

Finally, it might be not enough to know whether two objects are different. It would be better to see what is the difference, property by property (and
properties can be POJOs themselves).

This small library allows to compare two complex object, using default or custom rules, and provides feedback as another object,
which can be analyzed or easily rendered as JSON/XML.


