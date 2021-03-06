/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.drools.workbench.screens.guided.dtable.client.widget.analysis.cache;

import org.drools.workbench.screens.guided.dtable.client.widget.analysis.index.keys.Key;
import org.drools.workbench.screens.guided.dtable.client.widget.analysis.index.keys.UUIDKey;
import org.drools.workbench.screens.guided.dtable.client.widget.analysis.index.keys.UpdatableKey;
import org.drools.workbench.screens.guided.dtable.client.widget.analysis.index.keys.Value;
import org.junit.Before;
import org.junit.Test;

import static org.drools.workbench.screens.guided.dtable.client.widget.analysis.cache.Util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class KeyTreeMapTest {

    private KeyTreeMap<Person> map;
    private Person             toni;
    private Person             eder;
    private Person             michael;

    @Before
    public void setUp() throws Exception {
        map = new KeyTreeMap<>();

        toni = new Person( "Toni", 20 );
        eder = new Person( "Eder", 20 );
        michael = new Person( "Michael", 30 );

        put( toni );
        put( eder );
        put( michael );
    }

    @Test
    public void testFindByUUID() throws Exception {
        assertMapContent( map.get( UUIDKey.UNIQUE_UUID ), toni.uuidKey, eder.uuidKey, michael.uuidKey );
    }

    @Test( expected = IllegalArgumentException.class )
    public void testReAdd() throws Exception {
        put( toni );
    }

    @Test
    public void testFindByName() throws Exception {
        assertMapContent( map.get( KeyDefinition.newKeyDefinition().withId( "name" ).build() ), "Toni", "Eder", "Michael" );
    }

    @Test
    public void testFindByAge() throws Exception {
        final MultiMap<Value, Person> age = map.get( KeyDefinition.newKeyDefinition().withId( "age" ).build() );

        assertMapContent( age, 20, 20, 30 );
        assertTrue( age.get( new Value( 20 ) ).contains( toni ) );
        assertTrue( age.get( new Value( 20 ) ).contains( eder ) );
    }

    @Test
    public void testUpdateAge() throws Exception {
        final MultiMapChangeHandler changeHandler = mock( MultiMapChangeHandler.class );
        map.get( KeyDefinition.newKeyDefinition().withId( "age" ).build() ).addChangeListener( changeHandler );

        toni.setAge( 10 );

        final MultiMap<Value, Person> age = map.get( KeyDefinition.newKeyDefinition().withId( "age" ).build() );

        assertFalse( age.get( new Value( 20 ) ).contains( toni ) );
        assertTrue( age.get( new Value( 10 ) ).contains( toni ) );

    }

    @Test
    public void testRetract() throws Exception {

        toni.uuidKey.retract();

        assertMapContent( map.get( UUIDKey.UNIQUE_UUID ), eder.uuidKey, michael.uuidKey );
        assertMapContent( map.get( KeyDefinition.newKeyDefinition().withId( "name" ).build() ), "Eder", "Michael" );
        assertMapContent( map.get( KeyDefinition.newKeyDefinition().withId( "age" ).build() ), 20, 30 );
    }

    @Test
    public void testRemoveWhenItemDoesNotExist() throws Exception {
        final UUIDKey uuidKey = mock( UUIDKey.class );
        when( uuidKey.getKeyDefinition() ).thenReturn( UUIDKey.UNIQUE_UUID );
        when( uuidKey.getSingleValue() ).thenReturn( new Value( "DoesNotExist" ) );
        assertNull( map.remove( uuidKey ) );

        assertEquals( 3, map.get( UUIDKey.UNIQUE_UUID ).size() );
    }

    private void put( final Person person ) {
        map.put( person );
    }

    class Person implements HasKeys {

        private final UUIDKey uuidKey = new UUIDKey( this );

        final String name;

        private UpdatableKey ageKey;

        public Person( final String name,
                       final int age ) {
            this.name = name;
            this.ageKey = new UpdatableKey( KeyDefinition.newKeyDefinition().withId( "age" ).build(),
                                            age );
        }

        @Override
        public Key[] keys() {
            return new Key[]{
                    uuidKey,
                    new Key( KeyDefinition.newKeyDefinition().withId( "name" ).build(),
                             name ),
                    ageKey
            };
        }

        public void setAge( final int age ) {
            final UpdatableKey oldKey = ageKey;

            final UpdatableKey<Person> newKey = new UpdatableKey<>( KeyDefinition.newKeyDefinition().withId( "age" ).build(),
                                                                    age );
            ageKey = newKey;

            oldKey.update( newKey,
                           this );

        }
    }
}