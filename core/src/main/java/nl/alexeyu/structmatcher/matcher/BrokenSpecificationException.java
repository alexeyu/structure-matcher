package nl.alexeyu.structmatcher.matcher;

public class BrokenSpecificationException extends RuntimeException {
    
    public BrokenSpecificationException(String property, Object value, String specification) {
        super(String.format("The original value of %s is %s. It is against the specification: %s", 
                property, value, specification));
    }

}
