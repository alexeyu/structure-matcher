package nl.alexeyu.structmatcher.examples.bookstore;

import java.util.List;

public record BookSearchResult(SearchMetadata metadata, List<Book> books) {
}
