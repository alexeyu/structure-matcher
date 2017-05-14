package nl.alexeyu.structmatcher.examples.bookstore.model;

import java.util.List;

public class Book {
    
    private String title;
    
    private List<Author> authors;
    
    private int yearPublished;

    public Book() {
    }

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
