package nl.alexeyu.structmatcher.examples.bookstore;

import java.util.List;

public record Book(String title, List<Author> authors, String meta, PublishingInfo publishingInfo) {

    public Book {
        authors = authors == null ? null : List.copyOf(authors);
    }
}
