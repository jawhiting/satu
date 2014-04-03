/*
 * Copyright 2014 Kaaprotech Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kaaprotech.satu.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gs.collections.impl.factory.Lists;
import com.gs.collections.impl.factory.Maps;
import com.gs.collections.impl.factory.Sets;
import com.kaaprotech.satu.runtime.java.DeltaType;
import com.kaaprotech.satu.test.model.SatuTestEnum;
import com.kaaprotech.satu.test.model.SatuTestKey;
import com.kaaprotech.satu.test.model.SatuTestModel;

@SuppressWarnings("boxing")
public class TestSatuToJava {

    private final SatuTestKey KEY_1 = new SatuTestKey(
            1L,
            true,
            'a',
            (byte) 0,
            1.1f,
            Sets.immutable.of(SatuTestEnum.FirstEnumMember, SatuTestEnum.SecondEnumMember),
            Maps.immutable.of("Key1", 1.1d, "Key2", 2.2d));

    // Equal to KEY_1
    private final SatuTestKey KEY_2 = new SatuTestKey(
            1L,
            true, 'a',
            (byte) 0, 1.1f,
            Sets.immutable.of(SatuTestEnum.SecondEnumMember, SatuTestEnum.FirstEnumMember),
            Maps.immutable.of("Key2", 2.2d, "Key1", 1.1d));

    private final SatuTestKey KEY_3 = new SatuTestKey(
            1L,
            true,
            'a',
            (byte) 1, // Differs
            1.1f,
            Sets.immutable.of(SatuTestEnum.FirstEnumMember, SatuTestEnum.SecondEnumMember),
            Maps.immutable.of("Key1", 1.1d, "Key2", 2.2d));

    @Test
    public void testEnum() {
        assertEquals(4, SatuTestEnum.values().length);
        assertEquals("FirstEnumMember", SatuTestEnum.values()[0].name());
        assertEquals("SecondEnumMember", SatuTestEnum.values()[1].name());
        assertEquals("ThirdEnumMember", SatuTestEnum.values()[2].name());
        assertEquals("ForthEnumMember", SatuTestEnum.values()[3].name());
    }

    @Test
    public void testKey() {
        assertEquals(KEY_1, KEY_2);
        assertEquals(KEY_1.hashCode(), KEY_2.hashCode());
        assertEquals(0, KEY_1.compareTo(KEY_2));
        assertNotEquals(KEY_1, KEY_3);
        assertEquals(-1, KEY_1.compareTo(KEY_3));
    }

    @Test
    public void testModelDefaults() {
        final SatuTestModel model = SatuTestModel.newBuilder(1).build();
        assertEquals(Integer.valueOf(7), model.getIntFieldWithDefault());
        assertEquals(Double.valueOf(23.7d), model.getDoubleFieldWithDefault());
        assertEquals("~stringFieldWithDefault~", model.getStringFieldWithDefault());
        assertEquals(SatuTestEnum.ThirdEnumMember, model.getEnumFieldWithDefault());
    }

    @Test
    public void testModelDeltaBasic() {
        final SatuTestModel base = SatuTestModel.newBuilder(1).build();

        // No changes so no delta
        assertNull(base.toBuilder().reconcile());
        assertNull(base.toBuilder().setIntFieldWithDefault(base.getIntFieldWithDefault()).reconcile());

        final SatuTestModel.Delta delta1 = base.toBuilder().setIntFieldWithDefault(base.getIntFieldWithDefault() + 1).reconcile();
        assertNotNull(delta1);
        assertTrue(delta1.hasIntFieldWithDefault());
        assertEquals(Integer.valueOf(base.getIntFieldWithDefault() + 1), delta1.getIntFieldWithDefault());

        final SatuTestModel.Builder builder2 = base.toBuilder();
        builder2.getSetOfKeysField().addAll(Lists.mutable.of(KEY_1, KEY_2, KEY_3));
        final SatuTestModel.Delta delta2 = builder2.reconcile();
        assertNotNull(delta2);
        assertTrue(delta2.hasSetOfKeysField());
        assertEquals(2, delta2.getSetOfKeysField().size());
        assertEquals(DeltaType.ADD, delta2.getSetOfKeysField().toSortedList().get(0).getDeltaType());
        assertEquals(KEY_1, delta2.getSetOfKeysField().toSortedList().get(0).getKey());
        assertEquals(DeltaType.ADD, delta2.getSetOfKeysField().toSortedList().get(1).getDeltaType());
        assertEquals(KEY_3, delta2.getSetOfKeysField().toSortedList().get(1).getKey());

        final SatuTestModel.Builder builder3 = base.toBuilder();
        final SatuTestModel.Builder childBuilder = SatuTestModel.newBuilder(2).setStringField("Test");
        final SatuTestModel.Delta childDelta = childBuilder.toDelta(DeltaType.ADD);
        builder3.getMapOfModelsField().put(SatuTestEnum.ThirdEnumMember, childBuilder);
        final SatuTestModel.Delta delta3 = builder3.reconcile();
        assertNotNull(delta3);
        assertTrue(delta3.hasMapOfModelsField());
        assertEquals(1, delta3.getMapOfModelsField().size());
        assertEquals(DeltaType.ADD, delta3.getMapOfModelsField().getFirst().getDeltaType());
        assertEquals(SatuTestEnum.ThirdEnumMember, delta3.getMapOfModelsField().getFirst().getKey());
        assertEquals(childDelta, delta3.getMapOfModelsField().getFirst().getValue());
    }
}