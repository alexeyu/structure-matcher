package nl.alexeyu.structmatcher.json;

import java.util.List;

/**
 * A small public model for the archive end-to-end test. Public with public getters so the core
 * matcher can discover its properties by reflection across the module boundary. Exercises a simple
 * field ({@code Color}), a list element ({@code Tags[0]}) and a nested-structure field
 * ({@code Sub.Flag}).
 */
public class SampleStructure {

    private final String color;

    private final List<String> tags;

    private final SampleSub sub;

    public SampleStructure(String color, List<String> tags, SampleSub sub) {
        this.color = color;
        this.tags = tags;
        this.sub = sub;
    }

    public String getColor() {
        return color;
    }

    public List<String> getTags() {
        return tags;
    }

    public SampleSub getSub() {
        return sub;
    }

}
