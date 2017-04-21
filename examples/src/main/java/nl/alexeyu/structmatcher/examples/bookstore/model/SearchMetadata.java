package nl.alexeyu.structmatcher.examples.bookstore.model;

import java.util.List;

public class SearchMetadata {
    
    private final List<String> keywords;
    
    private final int booksFound;

    private final int processingTimeMs;
    
    private final Server server;

    public SearchMetadata(List<String> keywords, int booksFound, 
            Server server, int processingTimeMs) {
        this.keywords = keywords;
        this.booksFound = booksFound;
        this.server = server;
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

    public Server getServer() {
        return server;
    }
    
}
