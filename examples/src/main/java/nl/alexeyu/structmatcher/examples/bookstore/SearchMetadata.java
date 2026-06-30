package nl.alexeyu.structmatcher.examples.bookstore;

import java.util.List;

public record SearchMetadata(List<String> keywords, int booksFound, int processingTimeMs,
        Server server, Platform platform) {
}
