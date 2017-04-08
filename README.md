# structure-matcher

This is a simple utility to compare Plain Old Java Objects.
Sometimes it is necessary to detect whether two POJOs are equal "enough".
Unfortunately, they don't have the equals method, and it is not possible
or difficult to introduce it.
Moreover, sometimes the objects cannot be completely identical.
For instance, they represent a complex payload of an HTTP response, 
and this response contains a sequence number or processing time.
Finally, it might be not enough to know whether two objects are different:
it would be better to see what is the difference, property by property (and
properties can be POJOs themselves).


