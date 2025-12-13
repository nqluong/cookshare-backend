package com.backend.cookshare.authentication.enums;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

public class UserRoleTest {

    // ================================
    //       TEST ENUM BASIC
    // ================================

    @Test
    void testEnumValuesCount() {
        UserRole[] roles = UserRole.values();
        assertEquals(2, roles.length);
    }

    @Test
    void testEnumValues() {
        assertEquals(UserRole.USER, UserRole.valueOf("USER"));
        assertEquals(UserRole.ADMIN, UserRole.valueOf("ADMIN"));
    }

    @Test
    void testEnumNames() {
        assertEquals("USER", UserRole.USER.name());
        assertEquals("ADMIN", UserRole.ADMIN.name());
    }

    @Test
    void testEnumOrdinals() {
        assertEquals(0, UserRole.USER.ordinal());
        assertEquals(1, UserRole.ADMIN.ordinal());
    }

    @Test
    void testEnumToString() {
        assertEquals("USER", UserRole.USER.toString());
        assertEquals("ADMIN", UserRole.ADMIN.toString());
    }

    @Test
    void testEnumEquality() {
        assertEquals(UserRole.USER, UserRole.USER);
        assertEquals(UserRole.ADMIN, UserRole.ADMIN);
        assertNotEquals(UserRole.USER, UserRole.ADMIN);
    }

    @Test
    void testValueOfInvalidThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> UserRole.valueOf("INVALID"));
    }

    // ================================
    //       TEST VALUES CLASS
    // ================================

    @Test
    void testValuesConstantsContent() {
        assertEquals("USER", UserRole.Values.USER);
        assertEquals("ADMIN", UserRole.Values.ADMIN);
    }

    @Test
    void testValuesFieldsArePublicStaticFinal() throws Exception {
        Field userField = UserRole.Values.class.getField("USER");
        Field adminField = UserRole.Values.class.getField("ADMIN");

        assertTrue(Modifier.isPublic(userField.getModifiers()));
        assertTrue(Modifier.isStatic(userField.getModifiers()));
        assertTrue(Modifier.isFinal(userField.getModifiers()));

        assertTrue(Modifier.isPublic(adminField.getModifiers()));
        assertTrue(Modifier.isStatic(adminField.getModifiers()));
        assertTrue(Modifier.isFinal(adminField.getModifiers()));
    }

    @Test
    void testValuesClassExists() {
        Class<?> valuesClass = UserRole.Values.class;
        assertNotNull(valuesClass);
        assertEquals("Values", valuesClass.getSimpleName());
    }

    @Test
    void testValuesClassIsPublicStatic() {
        Class<?> valuesClass = UserRole.Values.class;
        assertTrue(Modifier.isPublic(valuesClass.getModifiers()));
        assertTrue(Modifier.isStatic(valuesClass.getModifiers()));
    }

    @Test
    void testValuesClassInstantiation() throws Exception {
        // Test để cover implicit constructor của Values class
        Constructor<UserRole.Values> constructor = UserRole.Values.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        UserRole.Values instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Test
    void testValuesConstantsMatchEnumNames() {
        assertEquals(UserRole.USER.name(), UserRole.Values.USER);
        assertEquals(UserRole.ADMIN.name(), UserRole.Values.ADMIN);
    }

    @Test
    void testValuesFieldCount() {
        Field[] fields = UserRole.Values.class.getDeclaredFields();
        long publicStaticFinalFields = 0;
        for (Field field : fields) {
            int modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers) &&
                    Modifier.isStatic(modifiers) &&
                    Modifier.isFinal(modifiers)) {
                publicStaticFinalFields++;
            }
        }
        assertEquals(2, publicStaticFinalFields);
    }
}