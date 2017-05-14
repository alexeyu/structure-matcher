package nl.alexeyu.structmatcher.examples.bookstore.model;

import java.util.List;

public class BookSearchResult {
    
    private SearchMetadata metadata;
    
    private List<Book> books;
    
    public BookSearchResult() {
    }

    public BookSearchResult(SearchMetadata metadata, List<Book> books) {
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
