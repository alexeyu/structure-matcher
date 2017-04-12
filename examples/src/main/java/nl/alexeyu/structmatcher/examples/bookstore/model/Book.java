package nl.alexeyu.structmatcher.examples.bookstore.model;

import java.util.List;

public class Book {
    
    private final String title;
    
    private final List<Author> authors;
    
    private final int yearPublished;

    public Book(String title, List<Author> authors, int yearPublished) {
        this.title = title;
        this.authors = authors;
        this.yearPublished = yearPublished;
    }

    public String getTitle() {
        return title;
    }

    public List<Author> getAuthors() {
        return authors;
    }

    public int getYearPublished() {
        return yearPublished;
    }
    
}
