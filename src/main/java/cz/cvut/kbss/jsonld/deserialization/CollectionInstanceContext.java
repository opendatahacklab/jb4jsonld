/**
 * Copyright (C) 2017 Czech Technical University in Prague
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.jsonld.deserialization;

import cz.cvut.kbss.jsonld.exception.JsonLdDeserializationException;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

class CollectionInstanceContext<T extends Collection> extends InstanceContext<T> {

    private final Class<?> targetType;

    CollectionInstanceContext(T instance, Map<String, Object> knownInstances) {
        super(instance, knownInstances);
        this.targetType = null;
    }

    CollectionInstanceContext(T instance, Class<?> targetType, Map<String, Object> knownInstances) {
        super(instance, knownInstances);
        this.targetType = targetType;
    }

    /**
     * Adds the specified item into this collection.
     *
     * @param item The item to add
     */
    @Override
    void addItem(Object item) {
        if (targetType == null) {
            instance.add(item);
            return;
        }
        Optional<Object> toAdd = resolveAssignableValue(targetType, item);
        if (!toAdd.isPresent()) {
            throw typeMismatch(targetType, item.getClass());
        } else {
            instance.add(toAdd.get());
        }
    }

    private JsonLdDeserializationException typeMismatch(Class<?> expected, Class<?> actual) {
        return new JsonLdDeserializationException(
                "Type mismatch. Unable to transform instance of type " + actual + " to the expected type " + expected);
    }

    @Override
    Class<?> getItemType() {
        return targetType;
    }
}
