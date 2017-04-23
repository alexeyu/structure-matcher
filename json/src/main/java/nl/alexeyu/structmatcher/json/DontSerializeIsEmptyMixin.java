package nl.alexeyu.structmatcher.json;

import com.fasterxml.jackson.annotation.JsonIgnore;

abstract class DontSerializeIsEmptyMixin {

    @JsonIgnore abstract boolean isEmpty();

}
