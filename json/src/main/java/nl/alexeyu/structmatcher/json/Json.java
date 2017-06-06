package nl.alexeyu.structmatcher.json;

import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public final class Json {
    
    private Json() {
    }
    
    /**
     * Produces a convenient mapper to convert feedback into JSON.
     */
    public static ObjectMapper mapper() {
        ObjectMapper mapper  = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY); 
        mapper.addMixIn(FeedbackNode.class, IgnoreFeedbackNodePropertiesMixin.class);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        SimpleModule module = new SimpleModule();
        module.addSerializer(new CompositeNodeSerializer());
        mapper.registerModule(module);
        return mapper;
    }

}
