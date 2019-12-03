package nl.alexeyu.structmatcher.examples.bookstore;

import java.util.Collections;
import java.util.List;

public final class Book {

    private String title;

    private List<Author> authors;

    private String meta;

    // From the version 2 only!
    private PublishingInfo publishingInfo;

    Book() {
    }

    Book(String title, List<Author> authors, String meta) {
        this.title = title;
        this.authors = Collections.unmodifiableList(authors);
        this.meta = meta;
    }

    public String getTitle() {
        return title;
    }

    public List<Author> getAuthors() {
        return authors;
    }

    public String getMeta() {
        return meta;
    }

    public PublishingInfo getPublishingInfo() {
        return publishingInfo;
    }

    public void setPublishingInfo(PublishingInfo publishingInfo) {
        this.publishingInfo = publishingInfo;
    }
}
