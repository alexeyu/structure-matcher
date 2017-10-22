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

```java
BookSearchResult desktopResponse = // read XML response
BookSearchResult mobileResponse = // read JSON response
FeedbackNode feedback = ObjectMatcher.forClass(BookSearchResult.class) 
        .with(Matchers.regex(IPADDRESS_PATTERN), "Metadata.Server.Ip")  // verifies that both properties are valid IP addresses
        .with(IntegerMatchers.oneOf(8080, 8081, 8090, 8091), "Metadata.Server.Port") // verifies that the port in the response is one of the values from the list
        .with(IntegerMatchers.inRange(2, 5000), "Metadata.ProcessingTimeMs") // verifies that the processing time is a reasonable number
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

### Pros and cons

The library is: 
* lightweight - no dependencies at all for the core module; json and examples modules depend on Jackson library.
* flexible - you can assign any matcher to any property of a composite object;
* extensible - you can write your matchers, however two dozen of them are already available,
covering many use cases.

The result can be converted to JSON and stored to a file which will make comparison of thousands of objects easy to read or parse.

Known limitations: only primitive objects, structures and lists are currently supported. Respectively, other collections (arrays, sets, enumerations, maps) cannot be used in a response model.
