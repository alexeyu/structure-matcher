package nl.alexeyu.structmatcher.examples.bookstore;

public final class Author {

    private String firstName;

    private String lastName;

    Author() {
    }

     Author(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

}
