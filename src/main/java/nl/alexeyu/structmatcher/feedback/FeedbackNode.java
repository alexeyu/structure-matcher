package nl.alexeyu.structmatcher.feedback;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface FeedbackNode {

    @JsonIgnore
    boolean isEmpty();

}
