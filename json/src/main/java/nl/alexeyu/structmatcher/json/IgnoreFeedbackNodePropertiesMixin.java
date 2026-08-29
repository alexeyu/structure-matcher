package nl.alexeyu.structmatcher.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Hides the internal fields from the rendering. {@code valueSlots} tells masking which slots of a
 * broken leaf hold data; it is bookkeeping rather than feedback.
 */
@JsonIgnoreProperties("valueSlots")
abstract class IgnoreFeedbackNodePropertiesMixin {

    @JsonIgnore
    abstract boolean isEmpty();

    @JsonIgnore
    abstract String getProperty();

}
