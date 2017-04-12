package nl.alexeyu.structmatcher.examples.bookstore.model;

import java.util.List;

public class SearchMetadata {
    
    private List<String> keywords;
    
    private final int booksFound;

    private final int processingTimeMs;

    public SearchMetadata(List<String> keywords, int booksFound, int processingTimeMs) {
        this.keywords = keywords;
        this.booksFound = booksFound;
        this.processingTimeMs = processingTimeMs;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public int getBooksFound() {
        return booksFound;
    }

    public int getProcessingTimeMs() {
        return processingTimeMs;
    }
    
}
