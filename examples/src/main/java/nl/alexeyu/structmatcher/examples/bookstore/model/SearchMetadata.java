package nl.alexeyu.structmatcher.examples.bookstore.model;

import java.util.List;

public class SearchMetadata {
    
    private List<String> keywords;
    
    private int booksFound;

    private int processingTimeMs;
    
    private Server server;
    
    private Platform platform;

    public SearchMetadata() {
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

    public Platform getPlatform() {
        return platform;
    }

}
