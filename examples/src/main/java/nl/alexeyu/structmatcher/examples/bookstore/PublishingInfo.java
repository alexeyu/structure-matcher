package nl.alexeyu.structmatcher.examples.bookstore;

import java.util.Objects;

public class PublishingInfo {

    private final String publisher;

    private final int year;

    private final int length;

    public PublishingInfo(String publisher, int year, int length) {
        this.publisher = publisher;
        this.year = year;
        this.length = length;
    }

    public String getPublisher() {
        return publisher;
    }

    public int getYear() {
        return year;
    }

    public int getLength() {
        return length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublishingInfo that = (PublishingInfo) o;
        return year == that.year &&
                length == that.length &&
                publisher.equals(that.publisher);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publisher, year, length);
    }

    @Override
    public String toString() {
        return "PublishingInfo{" +
                "publisher='" + publisher + '\'' +
                ", year=" + year +
                ", length=" + length +
                '}';
    }
}
