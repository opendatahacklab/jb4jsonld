package cz.cvut.kbss.jsonld.deserialization;

import cz.cvut.kbss.jsonld.common.BeanClassProcessor;
import cz.cvut.kbss.jsonld.deserialization.util.DataTypeTransformer;
import cz.cvut.kbss.jsonld.exception.JsonLdDeserializationException;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

class SingularObjectContext<T> extends InstanceContext<T> {

    private final Map<String, Field> fieldMap;

    SingularObjectContext(T instance, Map<String, Field> fieldMap, Map<String, Object> knownInstances) {
        super(instance, knownInstances);
        this.fieldMap = fieldMap;
    }

    @Override
    Field getFieldForProperty(String property) {
        // TODO Add handling of @Properties
        return fieldMap.get(property);
    }

    @Override
    void setFieldValue(Field field, Object value) {
        assert !(instance instanceof Collection);
        if (!field.getType().isAssignableFrom(value.getClass())) {
            boolean success = trySettingReferenceToKnownInstance(field, value);
            if (success) {
                return;
            }
            success = tryTypeTransformation(field, value);
            if (success) {
                return;
            }
            throw valueTypeMismatch(value, field);
        }
        BeanClassProcessor.setFieldValue(field, instance, value);
    }

    private JsonLdDeserializationException valueTypeMismatch(Object value, Field field) {
        return new JsonLdDeserializationException(
                "Type mismatch. Cannot set value " + value + " of type " + value.getClass() + " on field " + field);
    }

    private boolean trySettingReferenceToKnownInstance(Field field, Object value) {
        if (!knownInstances.containsKey(value.toString())) {
            return false;
        }
        final Object knownInstance = knownInstances.get(value.toString());
        if (!field.getType().isAssignableFrom(knownInstance.getClass())) {
            // Throw the exception right here so that it contains info about the known instance's type
            throw valueTypeMismatch(knownInstance, field);
        }
        BeanClassProcessor.setFieldValue(field, instance, knownInstance);
        return true;
    }

    private boolean tryTypeTransformation(Field field, Object value) {
        final Class<?> targetType = field.getType();
        final Object transformedValue = DataTypeTransformer.transformValue(value, targetType);
        if (transformedValue != null) {
            BeanClassProcessor.setFieldValue(field, instance, transformedValue);
            return true;
        }
        return false;
    }
}
