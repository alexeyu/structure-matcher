package nl.alexeyu.structmatcher.report;

import java.util.List;

/**
 * A small public model for the end-to-end report test. It must be public with public getters so the
 * core matcher can discover and invoke its properties by reflection across the module boundary. It
 * exercises a simple field ({@code Color}), a list element ({@code Tags[0]}) and a nested-structure
 * field ({@code Sub.Flag}).
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
