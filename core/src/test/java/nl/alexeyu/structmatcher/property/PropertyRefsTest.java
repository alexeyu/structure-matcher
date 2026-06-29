package nl.alexeyu.structmatcher.property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import nl.alexeyu.structmatcher.matcher.RecordSubstructure;
import nl.alexeyu.structmatcher.matcher.Structure;
import nl.alexeyu.structmatcher.matcher.Substructure;

public class PropertyRefsTest {

    @Test
    public void stripsGetPrefixFromBeanGetter() {
        assertEquals("Color", PropertyRefs.nameOf(Structure::getColor));
    }

    @Test
    public void stripsIsPrefixFromBooleanGetter() {
        assertEquals("Bool", PropertyRefs.nameOf(Substructure::isBool));
    }

    @Test
    public void capitalizesRecordComponentAccessor() {
        assertEquals("Bool", PropertyRefs.nameOf(RecordSubstructure::bool));
    }

    @Test
    public void typedAndRecordReferencesYieldTheSameName() {
        assertEquals(PropertyRefs.nameOf(Substructure::isBool),
                PropertyRefs.nameOf(RecordSubstructure::bool));
    }

    @Test
    public void rejectsInlineLambda() {
        assertThrows(IllegalArgumentException.class,
                () -> PropertyRefs.nameOf((Structure s) -> s.getColor()));
    }

}
