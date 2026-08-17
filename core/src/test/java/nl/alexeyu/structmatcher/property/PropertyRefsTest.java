package nl.alexeyu.structmatcher.property;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;
import java.net.URLClassLoader;

import org.junit.jupiter.api.Test;

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

    @Test
    public void resolvesAccessorWithoutAContextClassLoader() {
        withContextClassLoader(null,
                () -> assertEquals("Color", PropertyRefs.nameOf(Structure::getColor)));
    }

    @Test
    public void resolvesAccessorWhenTheContextClassLoaderCannotSeeTheModel() {
        var isolated = new URLClassLoader(new URL[0], null);
        withContextClassLoader(isolated,
                () -> assertEquals("Color", PropertyRefs.nameOf(Structure::getColor)));
    }

    private static void withContextClassLoader(ClassLoader loader, Runnable body) {
        var thread = Thread.currentThread();
        var previous = thread.getContextClassLoader();
        thread.setContextClassLoader(loader);
        try {
            body.run();
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

}
