package nl.alexeyu.structmatcher.matcher;

import java.util.List;

public class Structure {
    
    final Color color;
    
    final List<String> strings;

    final Substructure sub;
    
    public Structure(Color color, List<String> strings, Substructure sub) {
        this.color = color;
        this.strings = strings;
        this.sub = sub;
    }

    public Color getColor() {
        return color;
    }

    public Substructure getSub() {
        return sub;
    }

    public List<String> getStrings() {
        return strings;
    }
    
}