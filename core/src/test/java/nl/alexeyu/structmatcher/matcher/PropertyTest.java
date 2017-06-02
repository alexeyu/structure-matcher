package nl.alexeyu.structmatcher.matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import nl.alexeyu.structmatcher.matcher.Property;

public class PropertyTest {
    
    private Structure testStructure = new Structure(Color.WHITE, new ArrayList<>(), new Substructure(true));

    @Test
    public void doesNotCreateNonGetterProperty() throws NoSuchMethodException {
        assertFalse(Property.of(getStructMethod("toString")).isPresent());
    }

    @Test
    public void doesNotCreateBlacklistedProperty() throws NoSuchMethodException {
        assertFalse(Property.of(getStructMethod("getClass")).isPresent());
    }

    @Test
    public void booleanPropertyIsSimple() throws NoSuchMethodException {
        Optional<Property> property = Property.of(Substructure.class.getMethod("isBool"));
        assertTrue(property.isPresent());
        assertTrue(property.get().isSimple());
    }

    @Test
    public void isPropertyIsConsidered() throws NoSuchMethodException {
        Optional<Property> property = Property.of(Substructure.class.getMethod("isBool"));
        assertTrue(property.isPresent());
        assertEquals("Bool", property.get().getName());
    }

    @Test
    public void enumPropertyIsSimple() throws NoSuchMethodException {
        Optional<Property> property = Property.of(getStructMethod("getColor"));
        assertTrue(property.isPresent());
        assertTrue(property.get().isSimple());
        assertFalse(property.get().isList());
    }

    @Test
    public void substructPropertyIsNotSimple() throws NoSuchMethodException {
        Optional<Property> property = Property.of(getStructMethod("getSub"));
        assertTrue(property.isPresent());
        assertFalse(property.get().isSimple());
        assertFalse(property.get().isList());
    }

    @Test
    public void colorStringPropertyIsList() throws NoSuchMethodException {
        Optional<Property> property = Property.of(getStructMethod("getStrings"));
        assertTrue(property.isPresent());
        assertFalse(property.get().isSimple());
        assertTrue(property.get().isList());
    }

    @Test
    public void getPropertyIsConsidered() throws NoSuchMethodException {
        Optional<Property> property = Property.of(getStructMethod("getColor"));
        assertTrue(property.isPresent());
        assertEquals("Color", property.get().getName());
    }

    @Test
    public void getPropertyReturnsValue() throws NoSuchMethodException {
        Optional<Property> property = Property.of(getStructMethod("getColor"));
        assertTrue(property.isPresent());
        assertEquals(Color.WHITE, property.get().getValue(testStructure));
    }

    @Test
    public void returnsAllThePropertiesOfClass() {
        Set<String> propertyNames = Property.forClass(Structure.class)
                .map(p -> p.getName()).collect(Collectors.toSet());
        assertEquals(3, propertyNames.size());
        assertTrue(propertyNames.contains("Color"));
        assertTrue(propertyNames.contains("Sub"));
        assertTrue(propertyNames.contains("Strings"));
    }

    private Method getStructMethod(String methodName) throws NoSuchMethodException {
        return testStructure.getClass().getMethod(methodName);
    }

}