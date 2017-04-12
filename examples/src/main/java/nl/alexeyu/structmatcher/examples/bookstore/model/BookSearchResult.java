package nl.alexeyu.structmatcher.examples.bookstore.model;

import java.util.List;

public class BookSearchResult {
    
    private final SearchMetadata metadata;
    
    private final List<Book> books;
    
    private final String nextPageLink;

    private final String prevPageLink;

    public BookSearchResult(SearchMetadata metadata, List<Book> books, String nextPageLink, String prevPageLink) {
        this.metadata = metadata;
        this.books = books;
        this.nextPageLink = nextPageLink;
        this.prevPageLink = prevPageLink;
    }

    public SearchMetadata getMetadata() {
        return metadata;
    }

    public List<Book> getBooks() {
        return books;
    }

    public String getNextPageLink() {
        return nextPageLink;
    }

    public String getPrevPageLink() {
        return prevPageLink;
    }
    
}
