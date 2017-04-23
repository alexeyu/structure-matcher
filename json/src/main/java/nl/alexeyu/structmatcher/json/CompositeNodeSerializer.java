package nl.alexeyu.structmatcher.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import nl.alexeyu.structmatcher.feedback.CompositeFeedbackNode;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

final class CompositeNodeSerializer extends StdSerializer<CompositeFeedbackNode> {
    
    public CompositeNodeSerializer() {
        super(CompositeFeedbackNode.class, false);
    }

    @Override
    public void serialize(CompositeFeedbackNode node, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        gen.writeArrayFieldStart(node.getName());
        for (FeedbackNode child : node.getChildren()) {
            gen.writeObject(child);
        }
        gen.writeEndArray();
        gen.writeEndObject();
    }
    
}