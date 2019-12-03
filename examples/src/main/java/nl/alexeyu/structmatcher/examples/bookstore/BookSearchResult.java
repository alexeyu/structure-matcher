package nl.alexeyu.structmatcher.examples.bookstore;

import java.util.List;

public final class BookSearchResult {
    
    private SearchMetadata metadata;
    
    private List<Book> books;
    
    BookSearchResult() {
    }

    BookSearchResult(SearchMetadata metadata, List<Book> books) {
        this.metadata = metadata;
        this.books = books;
    }

    public SearchMetadata getMetadata() {
        return metadata;
    }

    public List<Book> getBooks() {
        return books;
    }

}
