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
package cz.cvut.kbss.jsonld.serialization.model;

import cz.cvut.kbss.jsonld.environment.Generator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;

public class ObjectIdNodeTest extends AbstractNodeTest {

    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    void writeValueOutputsTheValueAsObjectWithIdFieldAndStringValue() throws Exception {
        final String name = "test";
        final String value = Generator.generateUri().toString();
        final JsonNode node = new ObjectIdNode(name, value);
        node.write(serializerMock);

        final InOrder inOrder = inOrder(serializerMock);
        inOrder.verify(serializerMock).writeFieldName(name);
        inOrder.verify(serializerMock).writeString(value);
    }
}
