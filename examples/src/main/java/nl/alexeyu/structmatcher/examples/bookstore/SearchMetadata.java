package nl.alexeyu.structmatcher.examples.bookstore;

import java.util.List;

public final class SearchMetadata {
    
    private List<String> keywords;
    
    private int booksFound;

    private int processingTimeMs;
    
    private Server server;
    
    private Platform platform;

    SearchMetadata() {
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
