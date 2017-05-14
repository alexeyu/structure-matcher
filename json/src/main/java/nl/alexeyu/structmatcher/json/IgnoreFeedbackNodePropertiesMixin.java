package nl.alexeyu.structmatcher.json;

import com.fasterxml.jackson.annotation.JsonIgnore;

abstract class IgnoreFeedbackNodePropertiesMixin {

    @JsonIgnore abstract boolean isEmpty();
    
    @JsonIgnore abstract boolean getProperty();

}
