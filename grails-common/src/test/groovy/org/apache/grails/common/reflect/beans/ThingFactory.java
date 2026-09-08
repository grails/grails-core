/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.grails.common.reflect.beans;

/**
 * Hands out beans whose classes are not public. They live in a different package from
 * {@code ReflectionUtils} on purpose: in the same package the JVM access check passes and none of
 * these shapes would need widening at all.
 */
public final class ThingFactory {

    private ThingFactory() {
    }

    /** An anonymous implementation of a public interface. */
    public static PublicThing anonymousThing(final String name) {
        return new PublicThing() {

            @Override
            public String getName() {
                return name;
            }
        };
    }

    /** A package-private class that implements nothing, so a read has nowhere to resolve to. */
    public static Object standaloneThing(String name) {
        return new StandaloneThing(name);
    }

    /** A package-private class overriding a public method covariantly, so javac emits a bridge. */
    public static Object covariantThing(String value) {
        return new CovariantThing(value);
    }
}

class StandaloneThing {

    private final String name;

    StandaloneThing(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

class CovariantThing extends PublicCovariantBase {

    private final String value;

    CovariantThing(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
