/**
 * Copyright (C) 2017 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.jsonld.common;

import cz.cvut.kbss.jsonld.exception.BeanProcessingException;
import cz.cvut.kbss.jsonld.exception.TargetTypeException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.*;

public class BeanClassProcessor {

    private BeanClassProcessor() {
        throw new AssertionError();
    }

    /**
     * Extracts value of the specified field, from the specified instance.
     *
     * @param field    The field to extract value from
     * @param instance Instance containing the field
     * @return Field value, possibly {@code null}
     */
    public static Object getFieldValue(Field field, Object instance) {
        Objects.requireNonNull(field);
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        try {
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw new BeanProcessingException("Unable to extract value of field " + field, e);
        }
    }

    /**
     * Sets value of the specified field.
     *
     * @param field    The field to set
     * @param instance Instance on which the field will be set
     * @param value    The value to use
     */
    public static void setFieldValue(Field field, Object instance, Object value) {
        Objects.requireNonNull(field);
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        try {
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new BeanProcessingException("Unable to set value of field " + field, e);
        }
    }

    /**
     * Creates new instance of the specified class.
     *
     * @param cls The class to instantiate
     * @return New instance
     * @throws BeanProcessingException If the class is missing a public no-arg constructor
     */
    public static <T> T createInstance(Class<T> cls) {
        try {
            return cls.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new BeanProcessingException("Class " + cls + " is missing a public no-arg constructor.", e);
        }
    }

    /**
     * Creates collection of the specified type.
     *
     * @param type Collection type
     * @return New collection instance
     */
    public static Collection<?> createCollection(CollectionType type) {
        switch (type) {
            case LIST:
                return new ArrayList<>();
            case SET:
                return new HashSet<>();
            default:
                throw new IllegalArgumentException("Unsupported collection type " + type);
        }
    }

    /**
     * Creates a collection to fill the specified field.
     * <p>
     * I.e. the type of the collection is determined from the declared type of the field.
     *
     * @param field The field to create collection for
     * @return New collection instance
     */
    public static Collection<?> createCollection(Field field) {
        Objects.requireNonNull(field);
        return createCollection(field.getType());
    }

    /**
     * Creates an instance of the specified collection type.
     * <p>
     * Lists and sets are supported.
     *
     * @param collectionType Type of the collection
     * @return New collection instance
     */
    public static Collection<?> createCollection(Class<?> collectionType) {
        Objects.requireNonNull(collectionType);
        CollectionType type;
        if (Set.class.isAssignableFrom(collectionType)) {
            type = CollectionType.SET;
        } else if (List.class.isAssignableFrom(collectionType)) {
            type = CollectionType.LIST;
        } else {
            throw new IllegalArgumentException(collectionType + " is not a supported collection type.");
        }
        return createCollection(type);
    }

    /**
     * Determines the declared element type of collection represented by the specified field.
     *
     * @param field Field whose type is a collection
     * @return Declared element type of the collection
     */
    public static Class<?> getCollectionItemType(Field field) {
        Objects.requireNonNull(field);
        return getGenericType(field, 0);
    }

    private static Class<?> getGenericType(Field field, int paramIndex) {
        try {
            final ParameterizedType fieldType = (ParameterizedType) field.getGenericType();
            final Type typeArgument = fieldType.getActualTypeArguments()[paramIndex];
            if (typeArgument instanceof Class) {
                return (Class<?>) typeArgument;
            } else {
                return (Class<?>) ((ParameterizedType) typeArgument).getRawType();
            }
        } catch (ClassCastException e) {
            throw new BeanProcessingException("Field " + field + " is not of parametrized type.");
        }
    }

    /**
     * Determines the declared type of keys of the map represented by the specified field.
     *
     * @param field Map field
     * @return Declared type of values
     */
    public static Class<?> getMapKeyType(Field field) {
        return getGenericType(field, 0);
    }

    /**
     * Gets the declared type of values of the map represented by the specified field.
     *
     * @param field Map field
     * @return Declared type of values
     */
    public static Class<?> getMapValueType(Field field) {
        Objects.requireNonNull(field);
        try {
            return getGenericType(field, 1);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new BeanProcessingException("Unable to determine declared Map value type of field " + field + ".", e);
        }
    }

    /**
     * In case the map represent by the specified field has as value another generic type, this method retrieves this
     * generic type's actual first argument.
     * <p>
     * This implementation is supposed to determine value type of {@link cz.cvut.kbss.jopa.model.annotations.Properties}
     * fields with the following declaration {@code Map<String, Collection<?>>}, where the collection can be replaced by
     * a more specific type (List, Set) and the map key type can be also different.
     *
     * @param field Map field
     * @return Value type if present, {@code null} otherwise
     */
    public static Class<?> getMapGenericValueType(Field field) {
        try {
            final ParameterizedType fieldType = (ParameterizedType) field.getGenericType();
            final Type typeArgument = fieldType.getActualTypeArguments()[1];
            if (typeArgument instanceof Class) {
                if (Collection.class.isAssignableFrom((Class<?>) typeArgument)) {
                    // For raw value type - Map<?, Collection>
                    return null;
                }
                throw new BeanProcessingException("Expected map value type to be generic. Field: " + field);
            } else {
                final ParameterizedType valueType = (ParameterizedType) typeArgument;
                final Type actualType = valueType.getActualTypeArguments()[0];
                if (Class.class.isAssignableFrom(actualType.getClass())) {
                    // For Map<?, Collection<String>>
                    return Class.class.cast(actualType);
                }
                // For Map<?, Collection<?>>
                return null;
            }
        } catch (ClassCastException e) {
            throw new BeanProcessingException("Field " + field + " is not of parametrized type.");
        }
    }

    /**
     * Checks whether the specified field represents a collection.
     *
     * @param field The field to examine
     * @return Whether the field is a collection or not
     */
    public static boolean isCollection(Field field) {
        Objects.requireNonNull(field);
        return Collection.class.isAssignableFrom(field.getType());
    }

    /**
     * Checks that the properties field is a {@link Map}.
     *
     * @param field The field to check
     * @throws cz.cvut.kbss.jsonld.exception.TargetTypeException When the field is not a Map
     */
    public static void verifyPropertiesFieldType(Field field) {
        if (!Map.class.isAssignableFrom(field.getType())) {
            throw new TargetTypeException("@Properties field " + field + " must be a java.util.Map.");
        }
    }

    /**
     * Checks whether the specified type is a valid identifier type.
     * <p>
     * Valid identifiers in JOPA are: {@link URI}, {@link URL}, and {@link String}.
     *
     * @param cls Class to check
     * @return {@code true} if the specified class can be used as identifier field type, {@code false} otherwise
     */
    public static boolean isIdentifierType(Class<?> cls) {
        // TODO This should be in JOPA API and reused from there
        return URI.class.equals(cls) || URL.class.equals(cls) || String.class.equals(cls);
    }
}
