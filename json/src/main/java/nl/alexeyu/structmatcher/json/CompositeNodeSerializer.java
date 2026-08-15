package nl.alexeyu.structmatcher.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import nl.alexeyu.structmatcher.feedback.CompositeFeedbackNode;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Renders a composite as an object keyed by the child property names.
 * <p>
 * A set or a map names its nodes after the element or key <code>toString()</code>, so two distinct
 * entries can claim one name. Two fields of that name would make a document with a duplicate key,
 * and a parser that keeps the last one drops a real mismatch. The serializer writes namesakes
 * once, as an array under the shared name.
 */
final class CompositeNodeSerializer extends StdSerializer<CompositeFeedbackNode> {

    public CompositeNodeSerializer() {
        super(CompositeFeedbackNode.class, false);
    }

    @Override
    public void serialize(CompositeFeedbackNode node, JsonGenerator gen,
            SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        for (var entry : groupByProperty(node).entrySet()) {
            var namesakes = entry.getValue();
            if (namesakes.size() == 1) {
                gen.writeObjectField(entry.getKey(), namesakes.get(0));
            } else {
                gen.writeArrayFieldStart(entry.getKey());
                for (var child : namesakes) {
                    gen.writeObject(child);
                }
                gen.writeEndArray();
            }
        }
        gen.writeEndObject();
    }

    private LinkedHashMap<String, List<FeedbackNode>> groupByProperty(
            CompositeFeedbackNode node) {
        var byProperty = new LinkedHashMap<String, List<FeedbackNode>>();
        for (var child : node.getChildren()) {
            byProperty.computeIfAbsent(child.getProperty(), name -> new ArrayList<>()).add(child);
        }
        return byProperty;
    }

}
